package com.sqlbot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "dashscope")
public class DashScopeProperties {

    private String apiKey = "";
    private String baseUrl = "https://dashscope.aliyuncs.com";
    private String embeddingModel = "text-embedding-v2";
    private boolean enabled = true;
    private int batchSize = 25;
    private int connectTimeoutSeconds = 10;
    private int readTimeoutSeconds = 60;
}
