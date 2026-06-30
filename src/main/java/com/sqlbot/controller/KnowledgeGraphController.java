package com.sqlbot.controller;

import com.sqlbot.dto.GraphDataDTO;
import com.sqlbot.dto.ResponseResult;
import com.sqlbot.service.KnowledgeGraphService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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

    @PostMapping("/api/rebuild")
    @ResponseBody
    public ResponseResult<GraphDataDTO> rebuildFromWiki() {
        return ResponseResult.success("已从 Wiki 重建知识图谱", knowledgeGraphService.rebuildFromWiki());
    }

    @GetMapping("/api/wiki-pages")
    @ResponseBody
    public ResponseResult<List<Map<String, String>>> listWikiPages() {
        return ResponseResult.success(knowledgeGraphService.listWikiPages());
    }

    @GetMapping("/api/graph/wiki")
    @ResponseBody
    public ResponseResult<GraphDataDTO> getWikiSubgraph(@RequestParam String path) {
        return ResponseResult.success(knowledgeGraphService.getWikiSubgraph(path));
    }

    @GetMapping("/api/graph/{docId}")
    @ResponseBody
    public ResponseResult<GraphDataDTO> getGraphByDocument(@PathVariable Long docId) {
        return ResponseResult.success(knowledgeGraphService.generateGraphFromDocument(docId));
    }
}
