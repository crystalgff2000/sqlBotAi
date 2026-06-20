package com.sqlbot.controller;

import com.sqlbot.dto.GraphDataDTO;
import com.sqlbot.dto.ResponseResult;
import com.sqlbot.service.KnowledgeGraphService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/knowledge-graph")
public class KnowledgeGraphController {

    @Autowired
    private KnowledgeGraphService knowledgeGraphService;

    @GetMapping
    public String index(Model model) {
        model.addAttribute("title", "\u77e5\u8bc6\u56fe\u8c31");
        return "modules/knowledge-graph";
    }

    @GetMapping("/api/graph")
    @ResponseBody
    public ResponseResult<GraphDataDTO> getFullGraph() {
        return ResponseResult.success(knowledgeGraphService.getFullGraph());
    }

    @GetMapping("/api/graph/{docId}")
    @ResponseBody
    public ResponseResult<GraphDataDTO> getGraphByDocument(@PathVariable Long docId) {
        return ResponseResult.success(knowledgeGraphService.generateGraphFromDocument(docId));
    }
}
