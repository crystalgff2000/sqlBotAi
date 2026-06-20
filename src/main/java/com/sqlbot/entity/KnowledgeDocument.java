package com.sqlbot.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "knowledge_documents")
@Data
public class KnowledgeDocument {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String title;
    private String content;
    
    @Column(name = "file_path")
    private String filePath;
    
    @Column(name = "file_type")
    private String fileType;
    
    private String category;
    
    @Column(name = "chunk_count")
    private Integer chunkCount;
    
    @Column(name = "is_processed")
    private Boolean isProcessed;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        isProcessed = false;
        chunkCount = 0;
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
