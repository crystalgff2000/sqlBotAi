package com.sqlbot.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class QueryResultDTO {
    private List<String> columns = new ArrayList<>();
    private List<List<String>> rows = new ArrayList<>();
    private int rowCount;
    private boolean truncated;
    private String notice;
    private String executedSql;
    private String error;
    private long elapsedMs;
}
