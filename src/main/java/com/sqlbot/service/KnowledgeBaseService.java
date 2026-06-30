package com.sqlbot.service;

import com.sqlbot.config.RagProperties;
import com.sqlbot.dto.ChatAnswerDTO;
import com.sqlbot.entity.KnowledgeDocument;
import com.sqlbot.repository.KnowledgeDocumentRepository;
import com.sqlbot.service.rag.LocalRagService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseService.class);

    @Autowired
    private KnowledgeDocumentRepository documentRepository;

    @Autowired
    private RagProperties ragProperties;

    @Autowired
    private LocalRagService localRagService;

    @Autowired
    private WikiSearchService wikiSearchService;

    @Autowired
    private DeepSeekService deepSeekService;
    
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
    
    public ChatAnswerDTO answerQuestion(String question) {
        if (question == null || question.isBlank()) {
            return new ChatAnswerDTO("请输入有效的问题。", "error", new String[0]);
        }

        String trimmedQuestion = question.trim();

        if (ragProperties.isEnabled()) {
            try {
                LocalRagService.RagAnswer ragAnswer = localRagService.answer(trimmedQuestion);
                return new ChatAnswerDTO(ragAnswer.answer(), ragAnswer.source(), ragAnswer.references());
            } catch (Exception e) {
                log.error("Local RAG failed", e);
                return new ChatAnswerDTO("RAG 问答失败：" + e.getMessage(), "error", new String[0]);
            }
        }

        List<WikiSearchResult> wikiResults = wikiSearchService.search(trimmedQuestion);
        if (wikiSearchService.hasMatch(wikiResults)) {
            String answer = wikiSearchService.buildAnswer(trimmedQuestion, wikiResults);
            String[] references = wikiResults.stream()
                    .map(r -> "wiki/" + r.getRelativePath())
                    .toArray(String[]::new);
            return new ChatAnswerDTO(answer, "wiki", references);
        }

        log.info("No wiki match for question, falling back to DeepSeek: {}", trimmedQuestion);
        try {
            String answer = deepSeekService.chat(trimmedQuestion);
            return new ChatAnswerDTO(answer, "deepseek", new String[0]);
        } catch (Exception e) {
            log.error("DeepSeek fallback failed", e);
            return new ChatAnswerDTO(
                    "本地 Wiki 未找到相关内容，且 DeepSeek 调用失败：" + e.getMessage()
                            + "\n\n请检查 deepseek.api-key 配置，或尝试换一种问法。",
                    "error",
                    new String[0]
            );
        }
    }

    public int rebuildRagIndex() {
        return localRagService.rebuildIndex();
    }

    public int getRagChunkCount() {
        return localRagService.getChunkCount();
    }

    public boolean isRagEnabled() {
        return ragProperties.isEnabled();
    }

    public String getRagVectorStoreType() {
        return localRagService.getVectorStoreType();
    }

    public boolean isRagEmbeddingReady() {
        return localRagService.isEmbeddingReady();
    }
    
    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(lastDot + 1).toLowerCase() : "";
    }
}
