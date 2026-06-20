package com.sqlbot.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "knowledge_graph_edges")
@Data
public class KnowledgeGraphEdge {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "source_node_id")
    private String sourceNodeId;
    
    @Column(name = "target_node_id")
    private String targetNodeId;
    
    private String relation;
    private String properties;
    private Double weight;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
