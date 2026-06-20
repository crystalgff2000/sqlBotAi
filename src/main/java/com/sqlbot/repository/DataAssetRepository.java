package com.sqlbot.repository;

import com.sqlbot.entity.DataAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DataAssetRepository extends JpaRepository<DataAsset, Long> {
    List<DataAsset> findByType(String type);
    List<DataAsset> findByNameContaining(String keyword);
}
