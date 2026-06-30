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

    private static final String DASHSCOPE_EMBEDDING_PATH =
            "/api/v1/services/embeddings/text-embedding/text-embedding";

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
        return QwenEmbeddingModel.builder()
                .apiKey(properties.getApiKey())
                .modelName(properties.getEmbeddingModel())
                .baseUrl(normalizeDashScopeBaseUrl(properties.getBaseUrl()))
                .build();
    }

    static String normalizeOpenAiBaseUrl(String baseUrl) {
        String normalized = baseUrl == null ? "" : baseUrl.trim().replaceAll("/$", "");
        if (normalized.endsWith("/v1")) {
            return normalized;
        }
        return normalized + "/v1";
    }

    static String normalizeDashScopeBaseUrl(String baseUrl) {
        String normalized = baseUrl == null ? "" : baseUrl.trim().replaceAll("/$", "");
        if (normalized.endsWith(DASHSCOPE_EMBEDDING_PATH)) {
            return normalized;
        }
        return normalized + DASHSCOPE_EMBEDDING_PATH;
    }
}
