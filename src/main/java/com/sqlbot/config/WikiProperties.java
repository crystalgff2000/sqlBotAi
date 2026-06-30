package com.sqlbot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "wiki")
public class WikiProperties {

    private String searchPattern = "classpath:wiki/**/*.md";
    private int minMatchScore = 3;
    private int maxResults = 3;
    private int maxSnippetLength = 500;
    private int maxAnswerLength = 6000;
}
