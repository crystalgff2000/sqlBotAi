package com.sqlbot.service;

import com.sqlbot.entity.KnowledgeDocument;
import com.sqlbot.repository.KnowledgeDocumentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class KnowledgeBaseService {
    
    @Autowired
    private KnowledgeDocumentRepository documentRepository;
    
    private final String UPLOAD_DIR = "src/main/resources/knowledge-base/";
    
    public List<KnowledgeDocument> findAll() {
        return documentRepository.findAll();
    }
    
    public KnowledgeDocument findById(Long id) {
        return documentRepository.findById(id).orElse(null);
    }
    
    public KnowledgeDocument save(KnowledgeDocument doc) {
        return documentRepository.save(doc);
    }
    
    public void delete(Long id) {
        documentRepository.deleteById(id);
    }
    
    public KnowledgeDocument uploadDocument(MultipartFile file, String category) throws IOException {
        String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path filepath = Paths.get(UPLOAD_DIR + filename);
        
        Files.createDirectories(filepath.getParent());
        Files.write(filepath, file.getBytes());
        
        KnowledgeDocument doc = new KnowledgeDocument();
        doc.setTitle(file.getOriginalFilename());
        doc.setFilePath(filepath.toString());
        doc.setFileType(getFileExtension(file.getOriginalFilename()));
        doc.setCategory(category);
        doc.setIsProcessed(false);
        
        return documentRepository.save(doc);
    }
    
    public String extractContent(Long docId) throws IOException {
        KnowledgeDocument doc = findById(docId);
        if (doc == null) return "";
        
        File file = new File(doc.getFilePath());
        StringBuilder content = new StringBuilder();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        
        return content.toString();
    }
    
    public String answerQuestion(String question) {
        List<KnowledgeDocument> docs = documentRepository.findAll();
        if (docs.isEmpty()) {
            return "\u77e5\u8bc6\u5e93\u6682\u65e0\u6587\u6863\uff0c\u8bf7\u5148\u4e0a\u4f20\u6587\u6863\u3002";
        }
        
        StringBuilder response = new StringBuilder();
        response.append("\u57fa\u4e8e\u77e5\u8bc6\u5e93\u7684\u56de\u7b54:\n\n");
        response.append("\u95ee\u9898: ").append(question).append("\n\n");
        
        response.append("\u6839\u636e\u77e5\u8bc6\u5e93\u4e2d\u7684 ").append(docs.size()).append(" \u4efd\u6587\u6863\uff0c\u627e\u5230\u4ee5\u4e0b\u76f8\u5173\u4fe1\u606f:\n");
        
        int count = 0;
        for (KnowledgeDocument doc : docs) {
            if (count >= 3) break;
            response.append("- \u6587\u6863\u300a").append(doc.getTitle()).append("\u300b");
            response.append(" (\u5206\u7c7b: ").append(doc.getCategory()).append(")\n");
            count++;
        }
        
        response.append("\n\u5efa\u8bae\u67e5\u770b\u76f8\u5173\u6587\u6863\u83b7\u53d6\u8be6\u7ec6\u4fe1\u606f\u3002");
        
        return response.toString();
    }
    
    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(lastDot + 1).toLowerCase() : "";
    }
}
