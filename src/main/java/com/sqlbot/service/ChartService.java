package com.sqlbot.service;

import com.sqlbot.dto.ChartDataDTO;
import com.sqlbot.entity.Concept;
import com.sqlbot.entity.EntityItem;
import com.sqlbot.entity.DataAsset;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

@Service
public class ChartService {
    
    @Autowired
    private ConceptService conceptService;
    
    @Autowired
    private EntityService entityService;
    
    @Autowired
    private DataAssetService dataAssetService;
    
    public ChartDataDTO getConceptCategoryDistribution() {
        ChartDataDTO chart = new ChartDataDTO();
        chart.setTitle("\u6982\u5ff5\u5206\u7c7b\u5206\u5e03");
        chart.setType("pie");
        
        List<Concept> concepts = conceptService.findAll();
        Map<String, Long> distribution = concepts.stream()
            .collect(Collectors.groupingBy(Concept::getCategory, Collectors.counting()));
        
        ChartDataDTO.SeriesData series = new ChartDataDTO.SeriesData();
        series.setName("\u6570\u91cf");
        series.setType("pie");
        series.setData(new ArrayList<>(distribution.values()));
        
        chart.setXAxis(new ArrayList<>(distribution.keySet()));
        chart.setSeries(Arrays.asList(series));
        
        return chart;
    }
    
    public ChartDataDTO getEntityTypeTrend() {
        ChartDataDTO chart = new ChartDataDTO();
        chart.setTitle("\u5b9e\u4f53\u7c7b\u578b\u8d8b\u52bf");
        chart.setType("bar");
        
        List<String> months = Arrays.asList("1\u6708", "2\u6708", "3\u6708", "4\u6708", "5\u6708", "6\u6708");
        Random random = new Random();
        
        List<EntityItem> entities = entityService.findAll();
        Map<String, List<EntityItem>> byType = entities.stream()
            .collect(Collectors.groupingBy(EntityItem::getType));
        
        chart.setXAxis(months);
        
        List<ChartDataDTO.SeriesData> seriesList = new ArrayList<>();
        for (String type : byType.keySet()) {
            ChartDataDTO.SeriesData series = new ChartDataDTO.SeriesData();
            series.setName(type);
            series.setType("line");
            
            List<Object> data = new ArrayList<>();
            int baseCount = byType.get(type).size();
            for (int i = 0; i < 6; i++) {
                data.add(baseCount + random.nextInt(20));
            }
            series.setData(data);
            seriesList.add(series);
        }
        
        chart.setSeries(seriesList);
        return chart;
    }
    
    public ChartDataDTO getDataAssetStats() {
        ChartDataDTO chart = new ChartDataDTO();
        chart.setTitle("\u6570\u636e\u8d44\u4ea7\u7edf\u8ba1");
        chart.setType("bar");
        
        List<DataAsset> assets = dataAssetService.findAll();
        Map<String, Long> byFormat = assets.stream()
            .collect(Collectors.groupingBy(DataAsset::getFormat, Collectors.counting()));
        
        ChartDataDTO.SeriesData series = new ChartDataDTO.SeriesData();
        series.setName("\u6570\u91cf");
        series.setType("bar");
        series.setData(new ArrayList<>(byFormat.values()));
        
        chart.setXAxis(new ArrayList<>(byFormat.keySet()));
        chart.setSeries(Arrays.asList(series));
        
        return chart;
    }
}
