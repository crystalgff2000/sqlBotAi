package com.sqlbot.repository;

import com.sqlbot.entity.KnowledgeGraphEdge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KnowledgeGraphEdgeRepository extends JpaRepository<KnowledgeGraphEdge, Long> {
    List<KnowledgeGraphEdge> findBySourceNodeId(String sourceNodeId);
    List<KnowledgeGraphEdge> findByTargetNodeId(String targetNodeId);
}
