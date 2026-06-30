package com.sqlbot.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WikiPageDTO {
    private String relativePath;
    private String title;
    private String content;
    private String type;
    private String domain;
}
