package com.sqlbot.service;

import com.sqlbot.dto.GraphDataDTO;
import com.sqlbot.entity.KnowledgeDocument;
import com.sqlbot.entity.KnowledgeGraphEdge;
import com.sqlbot.entity.KnowledgeGraphNode;
import com.sqlbot.repository.KnowledgeGraphEdgeRepository;
import com.sqlbot.repository.KnowledgeGraphNodeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class KnowledgeGraphService {
    
    @Autowired
    private KnowledgeGraphNodeRepository nodeRepository;
    
    @Autowired
    private KnowledgeGraphEdgeRepository edgeRepository;
    
    @Autowired
    private WikiKnowledgeGraphService wikiKnowledgeGraphService;

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

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
    
    public GraphDataDTO generateGraphFromDocument(Long docId) {
        KnowledgeDocument doc = knowledgeBaseService.findById(docId);
        if (doc == null) return new GraphDataDTO();
        
        GraphDataDTO graph = new GraphDataDTO();
        List<GraphDataDTO.Node> nodes = new ArrayList<>();
        List<GraphDataDTO.Edge> edges = new ArrayList<>();
        
        // \u521b\u5efa\u4e2d\u5fc3\u8282\u70b9
        GraphDataDTO.Node centerNode = new GraphDataDTO.Node();
        centerNode.setId("doc_" + docId);
        centerNode.setName(doc.getTitle());
        centerNode.setCategory("\u6587\u6863");
        centerNode.setSymbolSize(60);
        Map<String, Object> centerStyle = new HashMap<>();
        centerStyle.put("color", "#5470c6");
        centerNode.setItemStyle(centerStyle);
        nodes.add(centerNode);
        
        // \u6a21\u62df\u4ece\u6587\u6863\u63d0\u53d6\u7684\u6982\u5ff5\u8282\u70b9
        String[] concepts = {"\u6838\u5fc3\u6982\u5ff5", "\u5173\u952e\u5b9e\u4f53", "\u91cd\u8981\u5173\u7cfb", "\u5e94\u7528\u573a\u666f", "\u6280\u672f\u539f\u7406"};
        String[] colors = {"#91cc75", "#fac858", "#ee6666", "#73c0de", "#3ba272"};
        
        for (int i = 0; i < concepts.length; i++) {
            GraphDataDTO.Node node = new GraphDataDTO.Node();
            node.setId("concept_" + i);
            node.setName(concepts[i]);
            node.setCategory("\u6982\u5ff5");
            node.setSymbolSize(40);
            
            Map<String, Object> style = new HashMap<>();
            style.put("color", colors[i % colors.length]);
            node.setItemStyle(style);
            nodes.add(node);
            
            // \u521b\u5efa\u4e0e\u4e2d\u5fc3\u8282\u70b9\u7684\u8fde\u63a5
            GraphDataDTO.Edge edge = new GraphDataDTO.Edge();
            edge.setSource(centerNode.getId());
            edge.setTarget(node.getId());
            edge.setRelation("\u5305\u542b");
            edges.add(edge);
        }
        
        // \u6dfb\u52a0\u5b50\u6982\u5ff5\u8282\u70b9
        for (int i = 0; i < concepts.length; i++) {
            for (int j = 0; j < 2; j++) {
                GraphDataDTO.Node subNode = new GraphDataDTO.Node();
                subNode.setId("sub_" + i + "_" + j);
                subNode.setName(concepts[i] + "-\u5b50\u9879" + (j + 1));
                subNode.setCategory("\u5b50\u6982\u5ff5");
                subNode.setSymbolSize(25);
                
                Map<String, Object> style = new HashMap<>();
                style.put("color", "#9a60b4");
                subNode.setItemStyle(style);
                nodes.add(subNode);
                
                GraphDataDTO.Edge edge = new GraphDataDTO.Edge();
                edge.setSource("concept_" + i);
                edge.setTarget(subNode.getId());
                edge.setRelation("\u7ec6\u5316");
                edges.add(edge);
            }
        }
        
        graph.setNodes(nodes);
        graph.setEdges(edges);
        
        return graph;
    }
    
    public GraphDataDTO getFullGraph() {
        return wikiKnowledgeGraphService.getFullGraphFromWiki();
    }
    
    private GraphDataDTO generateSampleGraph() {
        GraphDataDTO graph = new GraphDataDTO();
        List<GraphDataDTO.Node> nodes = new ArrayList<>();
        List<GraphDataDTO.Edge> edges = new ArrayList<>();
        
        String[] categories = {"\u6570\u636e\u5e93", "\u6570\u636e\u8868", "\u5b57\u6bb5", "\u7d22\u5f15", "\u5173\u7cfb"};
        String[] colors = {"#5470c6", "#91cc75", "#fac858", "#ee6666", "#73c0de"};
        int[] sizes = {60, 45, 30, 35, 40};
        
        for (int i = 0; i < categories.length; i++) {
            GraphDataDTO.Node node = new GraphDataDTO.Node();
            node.setId("node_" + i);
            node.setName(categories[i]);
            node.setCategory(categories[i]);
            node.setSymbolSize(sizes[i]);
            
            Map<String, Object> style = new HashMap<>();
            style.put("color", colors[i]);
            node.setItemStyle(style);
            nodes.add(node);
        }
        
        // \u521b\u5efa\u5173\u7cfb
        String[][] relations = {
            {"\u6570\u636e\u5e93", "\u5305\u542b", "\u6570\u636e\u8868"},
            {"\u6570\u636e\u8868", "\u62e5\u6709", "\u5b57\u6bb5"},
            {"\u6570\u636e\u8868", "\u4f7f\u7528", "\u7d22\u5f15"},
            {"\u6570\u636e\u8868", "\u5173\u8054", "\u5173\u7cfb"},
            {"\u5b57\u6bb5", "\u7ec4\u6210", "\u7d22\u5f15"}
        };
        
        for (int i = 0; i < relations.length; i++) {
            GraphDataDTO.Edge edge = new GraphDataDTO.Edge();
            edge.setSource("node_" + (i % categories.length));
            edge.setTarget("node_" + ((i + 1) % categories.length));
            edge.setRelation(relations[i][1]);
            edges.add(edge);
        }
        
        graph.setNodes(nodes);
        graph.setEdges(edges);
        return graph;
    }
    
    public GraphDataDTO rebuildFromWiki() {
        return wikiKnowledgeGraphService.rebuildFromWiki();
    }

    public GraphDataDTO getWikiSubgraph(String relativePath) {
        return wikiKnowledgeGraphService.getSubgraph(relativePath);
    }

    public List<Map<String, String>> listWikiPages() {
        return wikiKnowledgeGraphService.listWikiPages();
    }

    private int resolveSymbolSize(String category) {
        return switch (category) {
            case "概念" -> 35;
            case "实体" -> 40;
            case "数据资产" -> 30;
            case "指标" -> 32;
            case "来源" -> 28;
            case "参考" -> 30;
            case "域定义" -> 26;
            default -> 28;
        };
    }

    public void saveNode(KnowledgeGraphNode node) {
        nodeRepository.save(node);
    }
    
    public void saveEdge(KnowledgeGraphEdge edge) {
        edgeRepository.save(edge);
    }
}
