package com.sqlbot.config;

import dev.langchain4j.community.model.dashscope.QwenEmbeddingModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class LangChain4jConfig {

    @Bean
    @ConditionalOnProperty(prefix = "deepseek", name = "enabled", havingValue = "true", matchIfMissing = true)
    public ChatModel deepSeekChatModel(DeepSeekProperties properties) {
        return OpenAiChatModel.builder()
                .baseUrl(normalizeOpenAiBaseUrl(properties.getBaseUrl()))
                .apiKey(properties.getApiKey())
                .modelName(properties.getModel())
                .timeout(Duration.ofSeconds(properties.getReadTimeoutSeconds()))
                .build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "dashscope", name = "enabled", havingValue = "true", matchIfMissing = true)
    public EmbeddingModel dashScopeEmbeddingModel(DashScopeProperties properties) {
        QwenEmbeddingModel.QwenEmbeddingModelBuilder builder = QwenEmbeddingModel.builder()
                .apiKey(properties.getApiKey())
                .modelName(properties.getEmbeddingModel());
        String customBaseUrl = resolveDashScopeBaseUrl(properties.getBaseUrl());
        if (customBaseUrl != null) {
            builder.baseUrl(customBaseUrl);
        }
        return builder.build();
    }

    static String normalizeOpenAiBaseUrl(String baseUrl) {
        String normalized = baseUrl == null ? "" : baseUrl.trim().replaceAll("/$", "");
        if (normalized.endsWith("/v1")) {
            return normalized;
        }
        return normalized + "/v1";
    }

    /**
     * QwenEmbeddingModel / DashScope SDK 自带默认 endpoint。
     * 仅在用户显式配置非默认根地址时才传入 baseUrl，避免路径重复拼接。
     */
    static String resolveDashScopeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return null;
        }
        String normalized = baseUrl.trim().replaceAll("/$", "");
        if ("https://dashscope.aliyuncs.com".equals(normalized)
                || "http://dashscope.aliyuncs.com".equals(normalized)) {
            return null;
        }
        return normalized;
    }
}
