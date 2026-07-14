package com.sqlbot.controller;

import com.sqlbot.dto.ResponseResult;
import com.sqlbot.dto.WikiCatalogNode;
import com.sqlbot.dto.WikiPageViewDTO;
import com.sqlbot.service.WikiCatalogService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/wiki")
public class WikiController {

    private final WikiCatalogService wikiCatalogService;

    public WikiController(WikiCatalogService wikiCatalogService) {
        this.wikiCatalogService = wikiCatalogService;
    }

    @GetMapping
    public String index(Model model) {
        model.addAttribute("title", "知识库");
        model.addAttribute("pageCount", wikiCatalogService.getPageCount());
        return "modules/wiki";
    }

    @GetMapping("/api/catalog")
    @ResponseBody
    public ResponseResult<WikiCatalogNode> getCatalog() {
        return ResponseResult.success(wikiCatalogService.getCatalogTree());
    }

    @GetMapping("/api/page")
    @ResponseBody
    public ResponseResult<WikiPageViewDTO> getPage(@RequestParam(required = false) String path) {
        WikiPageViewDTO page = path == null || path.isBlank()
                ? wikiCatalogService.getDefaultPageView()
                : wikiCatalogService.getPageView(path.trim());

        if (page == null) {
            return ResponseResult.error(404, "未找到 Wiki 页面: " + path);
        }
        return ResponseResult.success(page);
    }

    @GetMapping("/api/stats")
    @ResponseBody
    public ResponseResult<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("pageCount", wikiCatalogService.getPageCount());
        return ResponseResult.success(stats);
    }
}
