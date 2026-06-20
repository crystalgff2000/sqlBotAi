package com.sqlbot.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "knowledge_graph_nodes")
@Data
public class KnowledgeGraphNode {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String nodeId;
    private String name;
    private String type;
    private String category;
    private String properties;
    
    @Column(name = "source_doc_id")
    private Long sourceDocId;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
