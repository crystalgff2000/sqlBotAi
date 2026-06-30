package com.sqlbot.service.rag;

import com.sqlbot.config.RagProperties;
import com.sqlbot.dto.RagChunkDTO;
import com.sqlbot.service.DeepSeekService;
import com.sqlbot.service.WikiSearchResult;
import com.sqlbot.service.WikiSearchService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@DependsOn("wikiSearchService")
public class LocalRagService {

    private static final Logger log = LoggerFactory.getLogger(LocalRagService.class);

    private final RagProperties ragProperties;
    private final WikiSearchService wikiSearchService;
    private final WikiChunkingService chunkingService;
    private final DeepSeekService deepSeekService;
    private final EmbeddingService embeddingService;
    private final EmbeddingVectorStore embeddingVectorStore;
    private final TfIdfVectorStore tfIdfVectorStore = new TfIdfVectorStore();
    private volatile boolean usingEmbeddingStore;

    public LocalRagService(
            RagProperties ragProperties,
            WikiSearchService wikiSearchService,
            WikiChunkingService chunkingService,
            DeepSeekService deepSeekService,
            EmbeddingService embeddingService,
            EmbeddingVectorStore embeddingVectorStore) {
        this.ragProperties = ragProperties;
        this.wikiSearchService = wikiSearchService;
        this.chunkingService = chunkingService;
        this.deepSeekService = deepSeekService;
        this.embeddingService = embeddingService;
        this.embeddingVectorStore = embeddingVectorStore;
    }

    @PostConstruct
    public void initIndex() {
        if (ragProperties.isRebuildOnStartup()) {
            rebuildIndex();
        }
    }

    public synchronized int rebuildIndex() {
        List<RagChunkDTO> chunks = chunkingService.chunkPages(wikiSearchService.getAllPages());
        if (useDashScopeEmbedding()) {
            try {
                embeddingVectorStore.buildIndex(chunks);
                usingEmbeddingStore = true;
                log.info("DashScope embedding index ready: {} chunks from {} wiki pages",
                        embeddingVectorStore.size(), wikiSearchService.getPageCount());
                return embeddingVectorStore.size();
            } catch (Exception e) {
                log.warn("DashScope embedding build failed, fallback to TF-IDF: {}", e.getMessage());
            }
        }

        tfIdfVectorStore.buildIndex(chunks);
        usingEmbeddingStore = false;
        log.info("Local TF-IDF index rebuilt: {} chunks from {} wiki pages",
                tfIdfVectorStore.size(), wikiSearchService.getPageCount());
        return tfIdfVectorStore.size();
    }

    public List<RagChunkDTO> retrieve(String question) {
        if (usingEmbeddingStore) {
            return embeddingVectorStore.search(question, ragProperties.getTopK(), ragProperties.getMinScore());
        }
        return tfIdfVectorStore.search(question, ragProperties.getTopK(), ragProperties.getTfidfMinScore());
    }

    public RagAnswer answer(String question) {
        List<WikiSearchResult> keywordResults = wikiSearchService.search(question);
        List<RagChunkDTO> vectorChunks = retrieve(question);
        List<RagChunkDTO> localChunks = mergeLocalChunks(keywordResults, vectorChunks);

        if (hasLocalKnowledge(keywordResults, vectorChunks)) {
            log.info("Local wiki match found, answering from knowledge base: {}", question);
            RagAnswer localAnswer = answerFromLocal(question, localChunks);
            if (isWikiNotFoundAnswer(localAnswer.answer())) {
                log.info("Local wiki context insufficient, fallback to DeepSeek: {}", question);
                return answerFromDeepSeek(question);
            }
            return localAnswer;
        }

        log.info("No local wiki match, fallback to DeepSeek: {}", question);
        return answerFromDeepSeek(question);
    }

    private RagAnswer answerFromDeepSeek(String question) {
        try {
            String answer = deepSeekService.chat(question);
            return new RagAnswer(answer, "deepseek", new String[0], List.of());
        } catch (Exception e) {
            throw new RuntimeException("DeepSeek 调用失败: " + e.getMessage(), e);
        }
    }

    private boolean isWikiNotFoundAnswer(String answer) {
        if (answer == null) {
            return true;
        }
        return answer.contains("本地 Wiki 知识库中未找到")
                || answer.contains("本地知识库中未找到")
                || answer.contains("知识库中未找到该问题");
    }

    public int getChunkCount() {
        if (usingEmbeddingStore) {
            return embeddingVectorStore.size();
        }
        return tfIdfVectorStore.size();
    }

    public String getVectorStoreType() {
        return usingEmbeddingStore ? "dashscope" : "tfidf";
    }

    public boolean isEmbeddingReady() {
        if (!usingEmbeddingStore) {
            return tfIdfVectorStore.size() > 0;
        }
        return embeddingService.isAvailable() && embeddingVectorStore.isReady();
    }

    private boolean useDashScopeEmbedding() {
        return "dashscope".equalsIgnoreCase(ragProperties.getVectorStore())
                && embeddingService.isAvailable();
    }

    private boolean hasLocalKnowledge(List<WikiSearchResult> keywordResults, List<RagChunkDTO> vectorChunks) {
        if (wikiSearchService.hasMatch(keywordResults)) {
            return true;
        }
        return !vectorChunks.isEmpty()
                && vectorChunks.get(0).getScore() >= getHighConfidenceScore();
    }

    private double getHighConfidenceScore() {
        return usingEmbeddingStore
                ? ragProperties.getHighConfidenceScore()
                : ragProperties.getTfidfHighConfidenceScore();
    }

    private RagAnswer answerFromLocal(String question, List<RagChunkDTO> localChunks) {
        if (localChunks.isEmpty()) {
            localChunks = retrieve(question);
        }

        String context = buildContext(localChunks);
        String[] references = extractReferences(localChunks);

        try {
            String answer = deepSeekService.chatWithWikiKnowledge(question, context);
            return new RagAnswer(answer, "wiki", references, localChunks);
        } catch (Exception e) {
            log.error("Wiki RAG generation failed, fallback to local excerpt", e);
            return new RagAnswer(buildExcerptAnswer(localChunks), "wiki", references, localChunks);
        }
    }

    private List<RagChunkDTO> mergeLocalChunks(List<WikiSearchResult> keywordResults, List<RagChunkDTO> vectorChunks) {
        Map<String, RagChunkDTO> merged = new LinkedHashMap<>();

        for (WikiSearchResult result : keywordResults) {
            if (!wikiSearchService.hasMatch(List.of(result))) {
                continue;
            }
            String content = buildKeywordChunkContent(result);
            double score = normalizeKeywordScore(result.getScore());
            RagChunkDTO chunk = new RagChunkDTO(
                    result.getRelativePath() + "#keyword",
                    result.getRelativePath(),
                    result.getTitle(),
                    content,
                    score
            );
            merged.put(result.getRelativePath(), chunk);
        }

        for (RagChunkDTO chunk : vectorChunks) {
            merged.merge(chunk.getSourcePath(), chunk, (existing, incoming) ->
                    incoming.getScore() > existing.getScore() ? incoming : existing);
        }

        List<RagChunkDTO> results = new ArrayList<>(merged.values());
        results.sort(Comparator.comparingDouble(RagChunkDTO::getScore).reversed());
        int limit = ragProperties.getTopK();
        if (results.size() > limit) {
            return results.subList(0, limit);
        }
        return results;
    }

    private double normalizeKeywordScore(int keywordScore) {
        if (usingEmbeddingStore) {
            return Math.min(0.99, 0.55 + keywordScore * 0.03);
        }
        return keywordScore / 10.0;
    }

    private String buildKeywordChunkContent(WikiSearchResult result) {
        StringBuilder content = new StringBuilder();
        content.append("文档: ").append(result.getTitle()).append("\n");
        content.append("路径: wiki/").append(result.getRelativePath()).append("\n\n");

        if (isTitleStrongMatch(result)) {
            String fullBody = wikiSearchService.getPageBody(result.getRelativePath());
            if (fullBody != null && !fullBody.isBlank()) {
                content.append(fullBody);
                return content.toString().trim();
            }
        }

        if (!result.getSnippets().isEmpty()) {
            for (String snippet : result.getSnippets()) {
                content.append(snippet).append("\n\n");
            }
        } else {
            content.append("（该文档与问题关键词匹配，请查阅完整内容。）");
        }
        return content.toString().trim();
    }

    private boolean isTitleStrongMatch(WikiSearchResult result) {
        String title = result.getTitle() == null ? "" : result.getTitle().toLowerCase(Locale.ROOT);
        return result.getScore() >= 5 && !title.isBlank();
    }

    private String buildContext(List<RagChunkDTO> chunks) {
        StringBuilder context = new StringBuilder();
        int maxLength = ragProperties.getMaxContextLength();

        for (int i = 0; i < chunks.size(); i++) {
            RagChunkDTO chunk = chunks.get(i);
            String block = "【片段 " + (i + 1) + " | 相关度 "
                    + String.format("%.2f", chunk.getScore()) + " | wiki/" + chunk.getSourcePath() + "】\n"
                    + chunk.getContent() + "\n\n";
            if (context.length() + block.length() > maxLength) {
                break;
            }
            context.append(block);
        }
        return context.toString().trim();
    }

    private String buildExcerptAnswer(List<RagChunkDTO> chunks) {
        StringBuilder answer = new StringBuilder("根据本地 Wiki 知识库找到以下相关内容：\n\n");
        Set<String> seen = new LinkedHashSet<>();
        for (RagChunkDTO chunk : chunks) {
            if (!seen.add(chunk.getSourcePath())) {
                continue;
            }
            answer.append("【").append(chunk.getTitle()).append("】\n");
            answer.append("路径: wiki/").append(chunk.getSourcePath()).append("\n");
            String excerpt = chunk.getContent();
            if (excerpt.length() > 600) {
                excerpt = excerpt.substring(0, 600) + "...";
            }
            answer.append(excerpt).append("\n\n");
        }
        return answer.toString().trim();
    }

    private String[] extractReferences(List<RagChunkDTO> chunks) {
        Set<String> refs = new LinkedHashSet<>();
        for (RagChunkDTO chunk : chunks) {
            refs.add("wiki/" + chunk.getSourcePath());
        }
        return refs.toArray(new String[0]);
    }

    public record RagAnswer(String answer, String source, String[] references, List<RagChunkDTO> chunks) {}
}
