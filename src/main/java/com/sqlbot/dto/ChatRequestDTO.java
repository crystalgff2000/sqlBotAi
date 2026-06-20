package com.sqlbot.dto;

import lombok.Data;

@Data
public class ChatRequestDTO {
    private String question;
    private String sessionId;
    private Long documentId;
}
