package com.sqlbot.service;

import com.sqlbot.entity.DataAsset;
import com.sqlbot.repository.DataAssetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DataAssetService {
    
    @Autowired
    private DataAssetRepository dataAssetRepository;
    
    public List<DataAsset> findAll() {
        return dataAssetRepository.findAll();
    }
    
    public DataAsset findById(Long id) {
        return dataAssetRepository.findById(id).orElse(null);
    }
    
    public DataAsset save(DataAsset asset) {
        return dataAssetRepository.save(asset);
    }
    
    public void delete(Long id) {
        dataAssetRepository.deleteById(id);
    }
    
    public List<DataAsset> findByType(String type) {
        return dataAssetRepository.findByType(type);
    }
    
    public List<DataAsset> search(String keyword) {
        return dataAssetRepository.findByNameContaining(keyword);
    }
    
    public Map<String, Long> getAssetTypeDistribution() {
        return findAll().stream()
            .collect(Collectors.groupingBy(DataAsset::getType, Collectors.counting()));
    }
    
    public Map<String, Long> getAssetFormatDistribution() {
        return findAll().stream()
            .collect(Collectors.groupingBy(DataAsset::getFormat, Collectors.counting()));
    }
}
