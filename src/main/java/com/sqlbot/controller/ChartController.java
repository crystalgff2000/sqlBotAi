package com.sqlbot.controller;

import com.sqlbot.dto.ChartDataDTO;
import com.sqlbot.dto.ResponseResult;
import com.sqlbot.service.ChartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/charts")
public class ChartController {

    @Autowired
    private ChartService chartService;

    @GetMapping
    public String charts(Model model) {
        model.addAttribute("title", "\u6570\u636e\u56fe\u8868");
        return "modules/charts";
    }

    @GetMapping("/api/concept-distribution")
    @ResponseBody
    public ResponseResult<ChartDataDTO> getConceptDistribution() {
        return ResponseResult.success(chartService.getConceptCategoryDistribution());
    }

    @GetMapping("/api/entity-trend")
    @ResponseBody
    public ResponseResult<ChartDataDTO> getEntityTrend() {
        return ResponseResult.success(chartService.getEntityTypeTrend());
    }

    @GetMapping("/api/asset-stats")
    @ResponseBody
    public ResponseResult<ChartDataDTO> getAssetStats() {
        return ResponseResult.success(chartService.getDataAssetStats());
    }

    @GetMapping("/api/all")
    @ResponseBody
    public ResponseResult<Map<String, ChartDataDTO>> getAllCharts() {
        Map<String, ChartDataDTO> charts = new HashMap<>();
        charts.put("conceptDistribution", chartService.getConceptCategoryDistribution());
        charts.put("entityTrend", chartService.getEntityTypeTrend());
        charts.put("assetStats", chartService.getDataAssetStats());
        return ResponseResult.success(charts);
    }
}
