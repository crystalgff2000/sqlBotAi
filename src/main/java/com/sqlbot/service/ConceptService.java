package com.sqlbot.service;

import com.sqlbot.entity.Concept;
import com.sqlbot.repository.ConceptRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ConceptService {
    
    @Autowired
    private ConceptRepository conceptRepository;
    
    public List<Concept> findAll() {
        return conceptRepository.findAll();
    }
    
    public Concept findById(Long id) {
        return conceptRepository.findById(id).orElse(null);
    }
    
    public Concept save(Concept concept) {
        return conceptRepository.save(concept);
    }
    
    public void delete(Long id) {
        conceptRepository.deleteById(id);
    }
    
    public List<Concept> findByCategory(String category) {
        return conceptRepository.findByCategory(category);
    }
    
    public List<Concept> search(String keyword) {
        return conceptRepository.findByNameContaining(keyword);
    }
}
