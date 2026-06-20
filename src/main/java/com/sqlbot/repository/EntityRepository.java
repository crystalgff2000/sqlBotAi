package com.sqlbot.repository;

import com.sqlbot.entity.EntityItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EntityRepository extends JpaRepository<EntityItem, Long> {
    List<EntityItem> findByType(String type);
    List<EntityItem> findByNameContaining(String keyword);
}
