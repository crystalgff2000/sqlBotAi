package com.sqlbot.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WikiPageViewDTO {
    private String relativePath;
    private String title;
    private String body;
    private String type;
    private String domain;
}
