package com.sqlbot.service;

import com.sqlbot.config.WikiProperties;
import com.sqlbot.dto.WikiPageDTO;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class WikiSearchService {

    private static final Logger log = LoggerFactory.getLogger(WikiSearchService.class);
    private static final Pattern ENGLISH_TOKEN = Pattern.compile("[a-zA-Z0-9_]{2,}");
    private static final Pattern CHINESE_SEGMENT = Pattern.compile("[\\u4e00-\\u9fff]{2,}");
    private static final Pattern TITLE_PATTERN = Pattern.compile("^title:\\s*[\"']?(.+?)[\"']?\\s*$", Pattern.MULTILINE);
    private static final Pattern TYPE_PATTERN = Pattern.compile("^type:\\s*[\"']?(.+?)[\"']?\\s*$", Pattern.MULTILINE);
    private static final Pattern DOMAIN_PATTERN = Pattern.compile("^domain:\\s*\\[?([^\\]\\n]+)\\]?\\s*$", Pattern.MULTILINE);

    private final WikiProperties wikiProperties;
    private final List<WikiDocument> documents = new ArrayList<>();

    public WikiSearchService(WikiProperties wikiProperties) {
        this.wikiProperties = wikiProperties;
    }

    @PostConstruct
    public void loadWikiDocuments() {
        documents.clear();
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

        try {
            Resource[] resources = resolver.getResources(wikiProperties.getSearchPattern());
            for (Resource resource : resources) {
                if (!shouldInclude(resource)) {
                    continue;
                }
                String content = readResource(resource);
                String relativePath = toRelativeWikiPath(resource);
                String title = extractTitle(content, relativePath);
                String type = extractField(content, TYPE_PATTERN);
                String domain = extractField(content, DOMAIN_PATTERN);
                documents.add(new WikiDocument(relativePath, title, content, type, domain));
            }
            log.info("Wiki knowledge base loaded: {} markdown files", documents.size());
        } catch (Exception e) {
            log.error("Failed to load wiki documents", e);
        }
    }

    public List<WikiSearchResult> search(String question) {
        List<String> keywords = extractKeywords(question);
        if (keywords.isEmpty()) {
            return List.of();
        }

        List<WikiSearchResult> results = new ArrayList<>();
        for (WikiDocument doc : documents) {
            int score = scoreDocument(doc, keywords);
            if (score <= 0) {
                continue;
            }

            WikiSearchResult result = new WikiSearchResult();
            result.setRelativePath(doc.relativePath);
            result.setTitle(doc.title);
            result.setScore(score);
            result.setSnippets(extractSnippets(doc.content, keywords));
            results.add(result);
        }

        results.sort(Comparator.comparingInt(WikiSearchResult::getScore).reversed());
        int limit = wikiProperties.getMaxResults();
        if (results.size() > limit) {
            return results.subList(0, limit);
        }
        return results;
    }

    public List<WikiPageDTO> getAllPages() {
        List<WikiPageDTO> pages = new ArrayList<>();
        for (WikiDocument doc : documents) {
            pages.add(new WikiPageDTO(doc.relativePath, doc.title, doc.content, doc.type, doc.domain));
        }
        return pages;
    }

    public int getPageCount() {
        return documents.size();
    }

    public WikiPageDTO getPageByPath(String relativePath) {
        for (WikiDocument doc : documents) {
            if (doc.relativePath.equals(relativePath)) {
                return new WikiPageDTO(doc.relativePath, doc.title, doc.content, doc.type, doc.domain);
            }
        }
        return null;
    }

    public String getPageBody(String relativePath) {
        WikiPageDTO page = getPageByPath(relativePath);
        if (page == null) {
            return null;
        }
        return stripFrontmatter(page.getContent());
    }

    private String extractField(String content, Pattern pattern) {
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "";
    }

    public boolean hasMatch(List<WikiSearchResult> results) {
        return !results.isEmpty() && results.get(0).getScore() >= wikiProperties.getMinMatchScore();
    }

    public String buildAnswer(String question, List<WikiSearchResult> results) {
        StringBuilder answer = new StringBuilder();
        answer.append("根据本地 Wiki 知识库找到以下相关内容：\n\n");

        int maxLength = wikiProperties.getMaxAnswerLength();
        for (WikiSearchResult result : results) {
            answer.append("【").append(result.getTitle()).append("】\n");
            answer.append("路径: wiki/").append(result.getRelativePath()).append("\n");

            if (!result.getSnippets().isEmpty()) {
                for (String snippet : result.getSnippets()) {
                    answer.append(snippet).append("\n\n");
                }
            } else {
                answer.append("（该文档与问题相关，请查阅完整内容。）\n\n");
            }

            if (answer.length() > maxLength) {
                break;
            }
        }

        if (answer.length() > maxLength) {
            return answer.substring(0, maxLength) + "\n\n（内容过长已截断，请查阅原文档获取完整信息。）";
        }
        return answer.toString().trim();
    }

    private boolean shouldInclude(Resource resource) {
        try {
            String path = resource.getURI().toString();
            if (path.contains("/.") || path.contains("/._")) {
                return false;
            }
            String filename = resource.getFilename();
            if (filename == null || filename.startsWith(".") || filename.startsWith("._")) {
                return false;
            }
            return filename.endsWith(".md");
        } catch (Exception e) {
            return false;
        }
    }

    private String readResource(Resource resource) throws Exception {
        try (InputStream is = resource.getInputStream()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private String toRelativeWikiPath(Resource resource) {
        try {
            String path = resource.getURI().toString();
            int wikiIndex = path.indexOf("/wiki/");
            if (wikiIndex >= 0) {
                return path.substring(wikiIndex + 6);
            }
            return resource.getFilename() != null ? resource.getFilename() : "unknown.md";
        } catch (Exception e) {
            return resource.getFilename() != null ? resource.getFilename() : "unknown.md";
        }
    }

    private String extractTitle(String content, String relativePath) {
        Matcher matcher = TITLE_PATTERN.matcher(content);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        for (String line : content.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("# ")) {
                return trimmed.substring(2).trim();
            }
        }

        String name = relativePath;
        int slash = name.lastIndexOf('/');
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        if (name.endsWith(".md")) {
            name = name.substring(0, name.length() - 3);
        }
        return name;
    }

    private List<String> extractKeywords(String question) {
        Set<String> keywords = new HashSet<>();
        String lower = question.toLowerCase(Locale.ROOT);

        Matcher englishMatcher = ENGLISH_TOKEN.matcher(lower);
        while (englishMatcher.find()) {
            keywords.add(englishMatcher.group());
        }

        Matcher chineseMatcher = CHINESE_SEGMENT.matcher(question);
        while (chineseMatcher.find()) {
            String segment = chineseMatcher.group();
            keywords.add(segment);
            if (segment.length() > 2) {
                for (int i = 0; i <= segment.length() - 2; i++) {
                    keywords.add(segment.substring(i, i + 2));
                }
            }
        }

        List<String> result = new ArrayList<>();
        for (String keyword : keywords) {
            if (keyword.length() >= 2) {
                result.add(keyword);
            }
        }
        return result;
    }

    private int scoreDocument(WikiDocument doc, List<String> keywords) {
        String titleLower = doc.title.toLowerCase(Locale.ROOT);
        String pathLower = doc.relativePath.toLowerCase(Locale.ROOT);
        String contentLower = doc.content.toLowerCase(Locale.ROOT);

        int score = 0;
        for (String keyword : keywords) {
            if (titleLower.contains(keyword)) {
                score += 5;
            }
            if (pathLower.contains(keyword)) {
                score += 3;
            }
            if (contentLower.contains(keyword)) {
                score += 1;
            }
        }
        return score;
    }

    private List<String> extractSnippets(String content, List<String> keywords) {
        List<String> snippets = new ArrayList<>();
        int maxSnippet = wikiProperties.getMaxSnippetLength();

        String body = stripFrontmatter(content);
        String[] blocks = body.split("\n\n");

        for (String block : blocks) {
            String trimmed = block.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            String blockLower = trimmed.toLowerCase(Locale.ROOT);
            boolean matched = false;
            for (String keyword : keywords) {
                if (blockLower.contains(keyword)) {
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                continue;
            }

            String snippet = trimmed.replaceAll("\n", " ");
            if (snippet.length() > maxSnippet) {
                snippet = snippet.substring(0, maxSnippet) + "...";
            }
            snippets.add(snippet);
            if (snippets.size() >= 2) {
                break;
            }
        }
        return snippets;
    }

    private String stripFrontmatter(String content) {
        if (content.startsWith("---")) {
            int end = content.indexOf("---", 3);
            if (end > 0) {
                return content.substring(end + 3);
            }
        }
        return content;
    }

    private record WikiDocument(String relativePath, String title, String content, String type, String domain) {}
}
