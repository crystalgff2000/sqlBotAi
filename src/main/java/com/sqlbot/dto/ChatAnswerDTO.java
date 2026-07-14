package com.sqlbot.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatAnswerDTO {

    private String answer;
    private String source;
    private String[] references;
    private String sql;
    private List<String> columns;
    private List<List<String>> rows;
    private String chartUrl;
    private Boolean truncated;

    public ChatAnswerDTO(String answer, String source, String[] references) {
        this(answer, source, references, null, null, null, null, null);
    }
}
