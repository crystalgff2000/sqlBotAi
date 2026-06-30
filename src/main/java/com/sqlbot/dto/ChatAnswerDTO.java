package com.sqlbot.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatAnswerDTO {

    private String answer;
    private String source;
    private String[] references;
}
