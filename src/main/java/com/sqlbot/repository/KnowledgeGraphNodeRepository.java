package com.sqlbot.repository;

import com.sqlbot.entity.KnowledgeGraphNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KnowledgeGraphNodeRepository extends JpaRepository<KnowledgeGraphNode, Long> {
    List<KnowledgeGraphNode> findByType(String type);
    List<KnowledgeGraphNode> findBySourceDocId(Long sourceDocId);
    KnowledgeGraphNode findByNodeId(String nodeId);
}
