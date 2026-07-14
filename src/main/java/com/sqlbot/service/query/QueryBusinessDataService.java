package com.sqlbot.service.query;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sqlbot.config.QueryBusinessDataProperties;
import com.sqlbot.config.RagProperties;
import com.sqlbot.dto.ChatAnswerDTO;
import com.sqlbot.dto.QueryResultDTO;
import com.sqlbot.dto.RagChunkDTO;
import com.sqlbot.dto.WikiPageDTO;
import com.sqlbot.service.DeepSeekService;
import com.sqlbot.service.WikiSearchResult;
import com.sqlbot.service.WikiSearchService;
import com.sqlbot.service.rag.LocalRagService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class QueryBusinessDataService {

    private static final Logger log = LoggerFactory.getLogger(QueryBusinessDataService.class);
    private static final Pattern SQL_BLOCK = Pattern.compile("```(?:sql)?\\s*([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);

    private final QueryBusinessDataProperties properties;
    private final RagProperties ragProperties;
    private final SkillPromptService skillPromptService;
    private final WikiSearchService wikiSearchService;
    private final LocalRagService localRagService;
    private final DeepSeekService deepSeekService;
    private final MySqlQueryExecutor mySqlQueryExecutor;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public QueryBusinessDataService(
            QueryBusinessDataProperties properties,
            RagProperties ragProperties,
            SkillPromptService skillPromptService,
            WikiSearchService wikiSearchService,
            LocalRagService localRagService,
            DeepSeekService deepSeekService,
            MySqlQueryExecutor mySqlQueryExecutor) {
        this.properties = properties;
        this.ragProperties = ragProperties;
        this.skillPromptService = skillPromptService;
        this.wikiSearchService = wikiSearchService;
        this.localRagService = localRagService;
        this.deepSeekService = deepSeekService;
        this.mySqlQueryExecutor = mySqlQueryExecutor;
    }

    public ChatAnswerDTO answer(String question) {
        String wikiContext = buildWikiContext(question);
        String[] references = extractReferences(question);
        String skillPrompt = skillPromptService.loadQuerySkillPrompt();

        try {
            String rawSql = deepSeekService.generateSql(question, wikiContext, skillPrompt);
            String sql = extractSql(rawSql);
            if (sql.isBlank()) {
                return new ChatAnswerDTO(
                        "未能根据 Wiki 知识生成有效 SQL，请补充指标、时间范围或统计粒度。",
                        "error",
                        references
                );
            }

            String sessionId = UUID.randomUUID().toString().replace("-", "");
            Path outputDir = Paths.get(properties.getOutputDir(), sessionId);
            QueryResultDTO queryResult = mySqlQueryExecutor.execute(sql, outputDir);
            if (queryResult.getError() != null && !queryResult.getError().isBlank()) {
                return new ChatAnswerDTO(
                        "业务查数失败：" + queryResult.getError(),
                        "error",
                        references,
                        queryResult.getExecutedSql() != null ? queryResult.getExecutedSql() : sql,
                        null,
                        null,
                        null,
                        false
                );
            }

            String resultJson = objectMapper.writeValueAsString(queryResult);
            String answer = deepSeekService.summarizeQueryResult(
                    question,
                    wikiContext,
                    resultJson,
                    queryResult.getExecutedSql(),
                    queryResult.isTruncated()
            );

            ChatAnswerDTO dto = new ChatAnswerDTO();
            dto.setAnswer(answer);
            dto.setSource("data-query");
            dto.setReferences(references);
            dto.setSql(queryResult.getExecutedSql());
            dto.setColumns(queryResult.getColumns());
            dto.setRows(queryResult.getRows());
            dto.setTruncated(queryResult.isTruncated());
            return dto;
        } catch (Exception e) {
            log.error("Query business data failed", e);
            return new ChatAnswerDTO(
                    "业务查数失败：" + e.getMessage(),
                    "error",
                    references
            );
        }
    }

    private String buildWikiContext(String question) {
        Set<String> seenPaths = new LinkedHashSet<>();
        StringBuilder context = new StringBuilder();
        int maxLength = ragProperties.getMaxContextLength();

        for (String path : wikiSearchService.resolveMandatoryOfflineMarketingPages(question)) {
            appendMandatoryPage(context, seenPaths, path, maxLength);
        }

        int searchLimit = Math.max(wikiSearchService.getMaxResultsForDataQuery(), 5);
        for (WikiSearchResult result : wikiSearchService.searchExpanded(question, searchLimit)) {
            if (!wikiSearchService.hasMatch(List.of(result)) || !seenPaths.add(result.getRelativePath())) {
                continue;
            }
            appendContextBlock(context, result.getTitle(), result.getRelativePath(), buildKeywordContent(result), maxLength);
        }

        for (RagChunkDTO chunk : safeRetrieve(question)) {
            if (!seenPaths.add(chunk.getSourcePath())) {
                continue;
            }
            appendContextBlock(context, chunk.getTitle(), chunk.getSourcePath(), chunk.getContent(), maxLength);
        }

        return context.toString().trim();
    }

    private void appendMandatoryPage(StringBuilder context, Set<String> seenPaths, String path, int maxLength) {
        if (!seenPaths.add(path)) {
            return;
        }
        WikiPageDTO page = wikiSearchService.getPageByPath(path);
        if (page == null) {
            return;
        }
        String body = wikiSearchService.getPageBody(path);
        if (body == null || body.isBlank()) {
            return;
        }
        appendContextBlock(context, page.getTitle(), path, body, maxLength);
    }

    private List<RagChunkDTO> safeRetrieve(String question) {
        try {
            return localRagService.retrieve(question);
        } catch (Exception e) {
            log.warn("Vector retrieval failed, fallback to keyword wiki context only: {}", e.getMessage());
            return List.of();
        }
    }

    private void appendContextBlock(
            StringBuilder context,
            String title,
            String path,
            String body,
            int maxLength) {
        String block = "【" + title + " | wiki/" + path + "】\n" + body + "\n\n";
        if (context.length() + block.length() > maxLength) {
            return;
        }
        context.append(block);
    }

    private String buildKeywordContent(WikiSearchResult result) {
        String fullBody = wikiSearchService.getPageBody(result.getRelativePath());
        if (fullBody != null && !fullBody.isBlank() && result.getScore() >= 5) {
            return fullBody;
        }
        if (!result.getSnippets().isEmpty()) {
            return String.join("\n\n", result.getSnippets());
        }
        return "（该文档与问题相关，请查阅完整内容。）";
    }

    private String[] extractReferences(String question) {
        Set<String> refs = new LinkedHashSet<>();
        for (String path : wikiSearchService.resolveMandatoryOfflineMarketingPages(question)) {
            refs.add("wiki/" + path);
        }
        int searchLimit = Math.max(wikiSearchService.getMaxResultsForDataQuery(), 5);
        for (WikiSearchResult result : wikiSearchService.searchExpanded(question, searchLimit)) {
            refs.add("wiki/" + result.getRelativePath());
        }
        for (RagChunkDTO chunk : safeRetrieve(question)) {
            refs.add("wiki/" + chunk.getSourcePath());
        }
        return refs.toArray(new String[0]);
    }

    private String extractSql(String raw) {
        if (raw == null) {
            return "";
        }
        Matcher matcher = SQL_BLOCK.matcher(raw);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        String trimmed = raw.trim();
        if (trimmed.toLowerCase(Locale.ROOT).startsWith("select")
                || trimmed.toLowerCase(Locale.ROOT).startsWith("with")) {
            return trimmed;
        }
        return "";
    }
}
