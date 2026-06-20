package com.sqlbot.repository;

import com.sqlbot.entity.Concept;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConceptRepository extends JpaRepository<Concept, Long> {
    List<Concept> findByCategory(String category);
    List<Concept> findByNameContaining(String keyword);
}
