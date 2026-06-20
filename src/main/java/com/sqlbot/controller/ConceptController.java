package com.sqlbot.controller;

import com.sqlbot.dto.ResponseResult;
import com.sqlbot.entity.Concept;
import com.sqlbot.service.ConceptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/concepts")
public class ConceptController {
    
    @Autowired
    private ConceptService conceptService;
    
    @GetMapping
    public String list(Model model) {
        model.addAttribute("concepts", conceptService.findAll());
        model.addAttribute("title", "\u6982\u5ff5\u7ba1\u7406");
        return "modules/concepts";
    }
    
    @GetMapping("/api/all")
    @ResponseBody
    public ResponseResult<List<Concept>> getAll() {
        return ResponseResult.success(conceptService.findAll());
    }
    
    @GetMapping("/api/{id}")
    @ResponseBody
    public ResponseResult<Concept> getById(@PathVariable Long id) {
        Concept concept = conceptService.findById(id);
        if (concept == null) {
            return ResponseResult.error("\u6982\u5ff5\u4e0d\u5b58\u5728");
        }
        return ResponseResult.success(concept);
    }
    
    @PostMapping("/api/save")
    @ResponseBody
    public ResponseResult<Concept> save(@RequestBody Concept concept) {
        return ResponseResult.success("\u4fdd\u5b58\u6210\u529f", conceptService.save(concept));
    }
    
    @DeleteMapping("/api/{id}")
    @ResponseBody
    public ResponseResult<String> delete(@PathVariable Long id) {
        conceptService.delete(id);
        return ResponseResult.success("\u5220\u9664\u6210\u529f", null);
    }
    
    @GetMapping("/api/search")
    @ResponseBody
    public ResponseResult<List<Concept>> search(@RequestParam String keyword) {
        return ResponseResult.success(conceptService.search(keyword));
    }
}
