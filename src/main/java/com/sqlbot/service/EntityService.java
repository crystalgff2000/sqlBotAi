package com.sqlbot.service;

import com.sqlbot.entity.EntityItem;
import com.sqlbot.repository.EntityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EntityService {
    
    @Autowired
    private EntityRepository entityRepository;
    
    public List<EntityItem> findAll() {
        return entityRepository.findAll();
    }
    
    public EntityItem findById(Long id) {
        return entityRepository.findById(id).orElse(null);
    }
    
    public EntityItem save(EntityItem entity) {
        return entityRepository.save(entity);
    }
    
    public void delete(Long id) {
        entityRepository.deleteById(id);
    }
    
    public List<EntityItem> findByType(String type) {
        return entityRepository.findByType(type);
    }
    
    public List<EntityItem> search(String keyword) {
        return entityRepository.findByNameContaining(keyword);
    }
}
