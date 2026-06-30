package com.sqlbot.service.rag;

import com.sqlbot.config.DashScopeProperties;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class DashScopeEmbeddingService implements EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(DashScopeEmbeddingService.class);
    /** text-embedding-v2 单条输入最大字符数 */
    private static final int MAX_INPUT_CHARS = 2048;

    private final DashScopeProperties properties;
    private final EmbeddingModel embeddingModel;

    public DashScopeEmbeddingService(
            DashScopeProperties properties,
            @Autowired(required = false) EmbeddingModel embeddingModel) {
        this.properties = properties;
        this.embeddingModel = embeddingModel;
    }

    @Override
    public boolean isAvailable() {
        return properties.isEnabled()
                && properties.getApiKey() != null
                && !properties.getApiKey().isBlank()
                && embeddingModel != null;
    }

    @Override
    public float[] embed(String text, TextType textType) {
        List<float[]> vectors = embedBatch(List.of(text), textType);
        if (vectors.isEmpty()) {
            throw new RuntimeException("DashScope Embedding 返回空向量");
        }
        return vectors.get(0);
    }

    @Override
    public List<float[]> embedBatch(List<String> texts, TextType textType) {
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }
        ensureAvailable();

        List<float[]> results = new ArrayList<>(texts.size());
        int batchSize = Math.max(1, properties.getBatchSize());

        for (int i = 0; i < texts.size(); i += batchSize) {
            List<String> batch = texts.subList(i, Math.min(i + batchSize, texts.size()));
            results.addAll(callEmbeddingApi(batch));
        }
        return results;
    }

    private List<float[]> callEmbeddingApi(List<String> texts) {
        List<TextSegment> segments = texts.stream()
                .map(this::truncate)
                .map(TextSegment::from)
                .toList();

        try {
            Response<List<Embedding>> response = embeddingModel.embedAll(segments);
            List<Embedding> embeddings = response.content();
            if (embeddings == null || embeddings.isEmpty()) {
                throw new RuntimeException("DashScope Embedding 返回空向量");
            }
            if (embeddings.size() != texts.size()) {
                throw new RuntimeException("Embedding 数量与输入数量不一致: "
                        + embeddings.size() + " vs " + texts.size());
            }

            List<float[]> vectors = new ArrayList<>(embeddings.size());
            for (Embedding embedding : embeddings) {
                vectors.add(normalize(embedding.vector()));
            }
            return vectors;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("DashScope Embedding call failed", e);
            throw new RuntimeException("DashScope Embedding 调用异常: " + e.getMessage(), e);
        }
    }

    private void ensureAvailable() {
        if (!isAvailable()) {
            throw new IllegalStateException("DashScope Embedding 未启用或未配置 API Key");
        }
    }

    static float[] normalize(float[] vector) {
        double norm = 0.0;
        for (float value : vector) {
            norm += value * value;
        }
        if (norm == 0.0) {
            return vector;
        }
        double sqrt = Math.sqrt(norm);
        float[] normalized = new float[vector.length];
        for (int i = 0; i < vector.length; i++) {
            normalized[i] = (float) (vector[i] / sqrt);
        }
        return normalized;
    }

    private String truncate(String text) {
        if (text == null || text.isBlank()) {
            return " ";
        }
        if (text.length() <= MAX_INPUT_CHARS) {
            return text;
        }
        return text.substring(0, MAX_INPUT_CHARS);
    }
}
