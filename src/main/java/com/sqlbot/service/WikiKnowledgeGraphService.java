package com.sqlbot.service;

import com.sqlbot.dto.GraphDataDTO;
import com.sqlbot.dto.WikiPageDTO;
import com.sqlbot.entity.KnowledgeGraphEdge;
import com.sqlbot.entity.KnowledgeGraphNode;
import com.sqlbot.repository.KnowledgeGraphEdgeRepository;
import com.sqlbot.repository.KnowledgeGraphNodeRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@DependsOn("wikiSearchService")
public class WikiKnowledgeGraphService {

    private static final Logger log = LoggerFactory.getLogger(WikiKnowledgeGraphService.class);

    private static final Pattern WIKI_LINK = Pattern.compile("\\[\\[([^\\]|]+)(?:\\|([^\\]]+))?\\]\\]");
    private static final Pattern MD_LINK = Pattern.compile("\\[([^\\]]+)\\]\\(([^)]+)\\)");
    private static final Pattern INLINE_LIST = Pattern.compile("^(source_assets|concept_refs):\\s*\\[(.+?)\\]\\s*$", Pattern.MULTILINE);

    private static final Map<String, String> CATEGORY_COLORS = Map.of(
            "概念", "#91cc75",
            "实体", "#fac858",
            "数据资产", "#5470c6",
            "指标", "#73c0de",
            "来源", "#ee6666",
            "参考", "#3ba272",
            "域定义", "#9a60b4",
            "文档", "#b6a2de"
    );

    private static final Map<String, Integer> CATEGORY_SIZES = Map.of(
            "概念", 35,
            "实体", 40,
            "数据资产", 30,
            "指标", 32,
            "来源", 28,
            "参考", 30,
            "域定义", 26,
            "文档", 24
    );

    private final WikiSearchService wikiSearchService;
    private final KnowledgeGraphNodeRepository nodeRepository;
    private final KnowledgeGraphEdgeRepository edgeRepository;

    public WikiKnowledgeGraphService(
            WikiSearchService wikiSearchService,
            KnowledgeGraphNodeRepository nodeRepository,
            KnowledgeGraphEdgeRepository edgeRepository) {
        this.wikiSearchService = wikiSearchService;
        this.nodeRepository = nodeRepository;
        this.edgeRepository = edgeRepository;
    }

    @PostConstruct
    public void initGraphOnStartup() {
        if (nodeRepository.count() == 0) {
            rebuildFromWiki();
        }
    }

    @Transactional
    public GraphDataDTO rebuildFromWiki() {
        List<WikiPageDTO> pages = wikiSearchService.getAllPages();
        GraphBuildResult buildResult = buildGraph(pages);

        nodeRepository.deleteAll();
        edgeRepository.deleteAll();

        for (GraphDataDTO.Node node : buildResult.nodes()) {
            KnowledgeGraphNode entity = new KnowledgeGraphNode();
            entity.setNodeId(node.getId());
            entity.setName(node.getName());
            entity.setType("wiki");
            entity.setCategory(node.getCategory());
            entity.setProperties("{}");
            nodeRepository.save(entity);
        }

        for (GraphDataDTO.Edge edge : buildResult.edges()) {
            KnowledgeGraphEdge entity = new KnowledgeGraphEdge();
            entity.setSourceNodeId(edge.getSource());
            entity.setTargetNodeId(edge.getTarget());
            entity.setRelation(edge.getRelation());
            entity.setWeight(1.0);
            edgeRepository.save(entity);
        }

        log.info("Wiki knowledge graph rebuilt: {} nodes, {} edges", buildResult.nodes().size(), buildResult.edges().size());

        GraphDataDTO graph = new GraphDataDTO();
        graph.setNodes(buildResult.nodes());
        graph.setEdges(buildResult.edges());
        return graph;
    }

    public List<Map<String, String>> listWikiPages() {
        List<Map<String, String>> result = new ArrayList<>();
        for (WikiPageDTO page : wikiSearchService.getAllPages()) {
            Map<String, String> item = new LinkedHashMap<>();
            item.put("path", page.getRelativePath());
            item.put("title", page.getTitle());
            item.put("category", resolveCategory(page));
            result.add(item);
        }
        return result;
    }

    public GraphDataDTO getFullGraphFromWiki() {
        GraphBuildResult buildResult = buildGraph(wikiSearchService.getAllPages());
        GraphDataDTO graph = new GraphDataDTO();
        graph.setNodes(buildResult.nodes());
        graph.setEdges(buildResult.edges());
        return graph;
    }

    public GraphDataDTO getSubgraph(String relativePath) {
        List<WikiPageDTO> pages = wikiSearchService.getAllPages();
        GraphBuildResult full = buildGraph(pages);
        String centerId = toNodeId(relativePath);

        if (full.nodeIndex().get(centerId) == null) {
            return new GraphDataDTO();
        }

        Set<String> included = new HashSet<>();
        included.add(centerId);
        for (GraphDataDTO.Edge edge : full.edges()) {
            if (centerId.equals(edge.getSource()) || centerId.equals(edge.getTarget())) {
                included.add(edge.getSource());
                included.add(edge.getTarget());
            }
        }

        GraphDataDTO graph = new GraphDataDTO();
        graph.setNodes(full.nodes().stream().filter(n -> included.contains(n.getId())).toList());
        graph.setEdges(full.edges().stream()
                .filter(e -> included.contains(e.getSource()) && included.contains(e.getTarget()))
                .toList());
        return graph;
    }

    private GraphBuildResult buildGraph(List<WikiPageDTO> pages) {
        Map<String, GraphDataDTO.Node> nodeIndex = new LinkedHashMap<>();
        Map<String, String> aliasIndex = new HashMap<>();
        List<GraphDataDTO.Edge> edges = new ArrayList<>();
        Set<String> edgeKeys = new HashSet<>();

        for (WikiPageDTO page : pages) {
            String nodeId = toNodeId(page.getRelativePath());
            String category = resolveCategory(page);

            GraphDataDTO.Node node = new GraphDataDTO.Node();
            node.setId(nodeId);
            node.setName(page.getTitle());
            node.setCategory(category);
            node.setWikiPath(page.getRelativePath());
            node.setSymbolSize(CATEGORY_SIZES.getOrDefault(category, 28));

            Map<String, Object> style = new HashMap<>();
            style.put("color", CATEGORY_COLORS.getOrDefault(category, "#b6a2de"));
            node.setItemStyle(style);

            nodeIndex.put(nodeId, node);
            registerAliases(aliasIndex, page.getRelativePath(), nodeId);
            registerAliases(aliasIndex, page.getTitle(), nodeId);
        }

        for (WikiPageDTO page : pages) {
            String sourceId = toNodeId(page.getRelativePath());
            addEdgesFromContent(page, sourceId, aliasIndex, nodeIndex, edges, edgeKeys, "引用");
            addEdgesFromFrontmatterLists(page, sourceId, aliasIndex, nodeIndex, edges, edgeKeys);
        }

        return new GraphBuildResult(new ArrayList<>(nodeIndex.values()), edges, nodeIndex);
    }

    private void addEdgesFromFrontmatterLists(
            WikiPageDTO page,
            String sourceId,
            Map<String, String> aliasIndex,
            Map<String, GraphDataDTO.Node> nodeIndex,
            List<GraphDataDTO.Edge> edges,
            Set<String> edgeKeys) {

        String content = page.getContent();
        if (!content.startsWith("---")) {
            return;
        }
        int end = content.indexOf("---", 3);
        if (end <= 0) {
            return;
        }
        String frontmatter = content.substring(0, end + 3);

        addListEdges(frontmatter, "source_assets", sourceId, "数据来源", aliasIndex, nodeIndex, edges, edgeKeys);
        addListEdges(frontmatter, "concept_refs", sourceId, "概念引用", aliasIndex, nodeIndex, edges, edgeKeys);
        addInlineListEdges(frontmatter, sourceId, aliasIndex, nodeIndex, edges, edgeKeys);
    }

    private void addInlineListEdges(
            String frontmatter,
            String sourceId,
            Map<String, String> aliasIndex,
            Map<String, GraphDataDTO.Node> nodeIndex,
            List<GraphDataDTO.Edge> edges,
            Set<String> edgeKeys) {

        Matcher matcher = INLINE_LIST.matcher(frontmatter);
        while (matcher.find()) {
            String relation = "concept_refs".equals(matcher.group(1)) ? "概念引用" : "数据来源";
            String[] items = matcher.group(2).split(",");
            for (String item : items) {
                addEdge(sourceId, item.trim(), relation, aliasIndex, nodeIndex, edges, edgeKeys);
            }
        }
    }

    private void addListEdges(
            String frontmatter,
            String field,
            String sourceId,
            String relation,
            Map<String, String> aliasIndex,
            Map<String, GraphDataDTO.Node> nodeIndex,
            List<GraphDataDTO.Edge> edges,
            Set<String> edgeKeys) {

        int fieldIndex = frontmatter.indexOf(field + ":");
        if (fieldIndex < 0) {
            return;
        }

        int lineEnd = frontmatter.indexOf('\n', fieldIndex);
        if (lineEnd < 0) {
            return;
        }

        int blockEnd = frontmatter.length();
        int nextField = frontmatter.indexOf("\n[a-zA-Z_]", lineEnd);
        if (nextField > 0) {
            blockEnd = nextField;
        }

        String block = frontmatter.substring(lineEnd, blockEnd);
        Matcher matcher = Pattern.compile("^\\s+-\\s+[\"']?(.+?)[\"']?\\s*$", Pattern.MULTILINE).matcher(block);
        while (matcher.find()) {
            String targetRef = matcher.group(1).trim();
            addEdge(sourceId, targetRef, relation, aliasIndex, nodeIndex, edges, edgeKeys);
        }
    }

    private void addEdgesFromContent(
            WikiPageDTO page,
            String sourceId,
            Map<String, String> aliasIndex,
            Map<String, GraphDataDTO.Node> nodeIndex,
            List<GraphDataDTO.Edge> edges,
            Set<String> edgeKeys,
            String defaultRelation) {

        String body = stripFrontmatter(page.getContent());
        String relation = inferRelationFromSection(body, defaultRelation);

        Matcher wikiMatcher = WIKI_LINK.matcher(body);
        while (wikiMatcher.find()) {
            addEdge(sourceId, wikiMatcher.group(1).trim(), relation, aliasIndex, nodeIndex, edges, edgeKeys);
        }

        Matcher mdMatcher = MD_LINK.matcher(body);
        while (mdMatcher.find()) {
            String target = mdMatcher.group(2).trim();
            if (target.startsWith("http") || target.startsWith("#")) {
                continue;
            }
            addEdge(sourceId, target, relation, aliasIndex, nodeIndex, edges, edgeKeys);
        }
    }

    private String inferRelationFromSection(String body, String defaultRelation) {
        int linkPos = body.indexOf("[[");
        if (linkPos < 0) {
            linkPos = body.indexOf("](");
        }
        if (linkPos < 0) {
            return defaultRelation;
        }

        String before = body.substring(0, linkPos);
        int headingPos = before.lastIndexOf("\n## ");
        if (headingPos >= 0) {
            String heading = before.substring(headingPos + 4).trim();
            if (heading.contains("相关概念")) {
                return "相关概念";
            }
            if (heading.contains("相关资产")) {
                return "相关资产";
            }
            if (heading.contains("血缘")) {
                return "血缘";
            }
        }
        return defaultRelation;
    }

    private void addEdge(
            String sourceId,
            String targetRef,
            String relation,
            Map<String, String> aliasIndex,
            Map<String, GraphDataDTO.Node> nodeIndex,
            List<GraphDataDTO.Edge> edges,
            Set<String> edgeKeys) {

        String targetId = resolveNodeId(targetRef, aliasIndex);
        if (targetId == null || sourceId.equals(targetId) || nodeIndex.get(targetId) == null) {
            return;
        }

        String key = sourceId + "->" + targetId + ":" + relation;
        if (!edgeKeys.add(key)) {
            return;
        }

        GraphDataDTO.Edge edge = new GraphDataDTO.Edge();
        edge.setSource(sourceId);
        edge.setTarget(targetId);
        edge.setRelation(relation);
        edges.add(edge);
    }

    private String resolveNodeId(String ref, Map<String, String> aliasIndex) {
        if (ref == null || ref.isBlank()) {
            return null;
        }

        String normalized = normalizeRef(ref);
        List<String> candidates = new ArrayList<>();
        candidates.add(normalized);
        candidates.add(normalized + ".md");
        candidates.add("pages/" + normalized);
        candidates.add("pages/" + normalized + ".md");

        if (normalized.startsWith("pages/")) {
            candidates.add(normalized.substring(6));
            candidates.add(normalized.substring(6) + ".md");
        }

        for (String candidate : candidates) {
            String nodeId = aliasIndex.get(candidate.toLowerCase(Locale.ROOT));
            if (nodeId != null) {
                return nodeId;
            }
        }

        return aliasIndex.get(normalized.toLowerCase(Locale.ROOT));
    }

    private void registerAliases(Map<String, String> aliasIndex, String alias, String nodeId) {
        if (alias == null || alias.isBlank()) {
            return;
        }

        String normalized = normalizeRef(alias);
        aliasIndex.putIfAbsent(normalized.toLowerCase(Locale.ROOT), nodeId);

        if (normalized.endsWith(".md")) {
            aliasIndex.putIfAbsent(normalized.substring(0, normalized.length() - 3).toLowerCase(Locale.ROOT), nodeId);
        }

        int slash = normalized.lastIndexOf('/');
        if (slash >= 0) {
            aliasIndex.putIfAbsent(normalized.substring(slash + 1).toLowerCase(Locale.ROOT), nodeId);
        }
    }

    private String normalizeRef(String ref) {
        return ref.trim()
                .replace('\\', '/')
                .replaceAll("^/+", "")
                .replaceAll("/+$", "");
    }

    private String toNodeId(String relativePath) {
        return "wiki:" + normalizeRef(relativePath);
    }

    private String resolveCategory(WikiPageDTO page) {
        if (page.getType() != null && !page.getType().isBlank()) {
            return switch (page.getType()) {
                case "concept" -> "概念";
                case "entity" -> "实体";
                case "data-asset-table", "data-asset-metric" -> page.getType().contains("metric") ? "指标" : "数据资产";
                case "data-asset-overview", "index", "overview" -> "文档";
                case "llm-query-reference" -> "参考";
                default -> mapPathCategory(page.getRelativePath());
            };
        }
        return mapPathCategory(page.getRelativePath());
    }

    private String mapPathCategory(String relativePath) {
        String path = relativePath.replace('\\', '/');
        if (path.contains("/concepts/")) {
            return "概念";
        }
        if (path.contains("/entities/")) {
            return "实体";
        }
        if (path.contains("/指标/")) {
            return "指标";
        }
        if (path.contains("/数据资产/") || path.contains("/表/")) {
            return "数据资产";
        }
        if (path.contains("/sources/")) {
            return "来源";
        }
        if (path.contains("/references/")) {
            return "参考";
        }
        if (path.endsWith("_domain.md") || path.endsWith("_category.md") || path.endsWith("_dimension.md")) {
            return "域定义";
        }
        return "文档";
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

    private record GraphBuildResult(
            List<GraphDataDTO.Node> nodes,
            List<GraphDataDTO.Edge> edges,
            Map<String, GraphDataDTO.Node> nodeIndex) {
    }
}
