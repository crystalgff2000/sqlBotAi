package com.sqlbot.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class GraphDataDTO {
    private List<Node> nodes;
    private List<Edge> edges;
    
    @Data
    public static class Node {
        private String id;
        private String name;
        private String category;
        private String wikiPath;
        private Integer symbolSize;
        private Map<String, Object> itemStyle;
        private Map<String, Object> label;
    }
    
    @Data
    public static class Edge {
        private String source;
        private String target;
        private String relation;
        private Map<String, Object> lineStyle;
        private Map<String, Object> label;
    }
}
