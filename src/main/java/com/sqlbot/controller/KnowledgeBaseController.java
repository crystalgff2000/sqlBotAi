package com.sqlbot.controller;

import com.sqlbot.dto.ChatRequestDTO;
import com.sqlbot.dto.ResponseResult;
import com.sqlbot.entity.KnowledgeDocument;
import com.sqlbot.service.KnowledgeBaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Controller
@RequestMapping("/knowledge-base")
public class KnowledgeBaseController {

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    @GetMapping
    public String index(Model model) {
        model.addAttribute("title", "\u77e5\u8bc6\u5e93\u95ee\u7b54");
        model.addAttribute("documents", knowledgeBaseService.findAll());
        return "modules/knowledge-base";
    }

    @GetMapping("/api/documents")
    @ResponseBody
    public ResponseResult<List<KnowledgeDocument>> getDocuments() {
        return ResponseResult.success(knowledgeBaseService.findAll());
    }

    @PostMapping("/api/upload")
    @ResponseBody
    public ResponseResult<KnowledgeDocument> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("category") String category) {
        try {
            KnowledgeDocument doc = knowledgeBaseService.uploadDocument(file, category);
            return ResponseResult.success("\u4e0a\u4f20\u6210\u529f", doc);
        } catch (IOException e) {
            return ResponseResult.error("\u4e0a\u4f20\u5931\u8d25: " + e.getMessage());
        }
    }

    @PostMapping("/api/chat")
    @ResponseBody
    public ResponseResult<String> chat(@RequestBody ChatRequestDTO request) {
        String answer = knowledgeBaseService.answerQuestion(request.getQuestion());
        return ResponseResult.success(answer);
    }

    @DeleteMapping("/api/documents/{id}")
    @ResponseBody
    public ResponseResult<String> deleteDocument(@PathVariable Long id) {
        knowledgeBaseService.delete(id);
        return ResponseResult.success("\u5220\u9664\u6210\u529f", null);
    }
}
