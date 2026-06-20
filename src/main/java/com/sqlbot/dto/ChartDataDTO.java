package com.sqlbot.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class ChartDataDTO {
    private String title;
    private String type;
    private List<String> xAxis;
    private List<SeriesData> series;
    private Map<String, Object> extra;
    
    @Data
    public static class SeriesData {
        private String name;
        private String type;
        private List<Object> data;
    }
}
