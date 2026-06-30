package com.sqlbot.service.rag;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.sqlbot.config.DashScopeProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class DashScopeEmbeddingService implements EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(DashScopeEmbeddingService.class);
    /** text-embedding-v2 单条输入最大字符数 */
    private static final int MAX_INPUT_CHARS = 2048;

    private final DashScopeProperties properties;
    private final Gson gson = new Gson();
    private final HttpClient httpClient;

    public DashScopeEmbeddingService(DashScopeProperties properties) {
        this.properties = properties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(properties.getConnectTimeoutSeconds()))
                .build();
    }

    @Override
    public boolean isAvailable() {
        return properties.isEnabled()
                && properties.getApiKey() != null
                && !properties.getApiKey().isBlank();
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
        if (!isAvailable()) {
            throw new IllegalStateException("DashScope Embedding 未启用或未配置 API Key");
        }

        List<float[]> results = new ArrayList<>(texts.size());
        int batchSize = Math.max(1, properties.getBatchSize());

        for (int i = 0; i < texts.size(); i += batchSize) {
            List<String> batch = texts.subList(i, Math.min(i + batchSize, texts.size()));
            results.addAll(callEmbeddingApi(batch, textType));
        }
        return results;
    }

    private List<float[]> callEmbeddingApi(List<String> texts, TextType textType) {
        String url = properties.getBaseUrl().replaceAll("/$", "")
                + "/api/v1/services/embeddings/text-embedding/text-embedding";

        JsonArray textsArray = new JsonArray();
        for (String text : texts) {
            textsArray.add(truncate(text));
        }

        JsonObject input = new JsonObject();
        input.add("texts", textsArray);

        JsonObject parameters = new JsonObject();
        parameters.addProperty("text_type", textType == TextType.QUERY ? "query" : "document");

        JsonObject body = new JsonObject();
        body.addProperty("model", properties.getEmbeddingModel());
        body.add("input", input);
        body.add("parameters", parameters);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(properties.getReadTimeoutSeconds()))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + properties.getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String responseBody = response.body();

            if (response.statusCode() != 200) {
                log.error("DashScope Embedding error: status={}, body={}", response.statusCode(), responseBody);
                throw new RuntimeException("DashScope Embedding 调用失败 (HTTP " + response.statusCode() + ")");
            }

            JsonObject json = gson.fromJson(responseBody, JsonObject.class);
            if (json.has("code")) {
                String code = json.get("code").getAsString();
                if (code != null && !code.isBlank() && !"Success".equalsIgnoreCase(code)) {
                    String message = json.has("message") ? json.get("message").getAsString() : responseBody;
                    throw new RuntimeException("DashScope Embedding 错误: " + message);
                }
            }

            JsonObject output = json.getAsJsonObject("output");
            if (output == null || !output.has("embeddings")) {
                throw new RuntimeException("DashScope Embedding 返回格式异常");
            }

            JsonArray embeddings = output.getAsJsonArray("embeddings");
            List<IndexedEmbedding> indexed = new ArrayList<>();
            for (JsonElement element : embeddings) {
                JsonObject item = element.getAsJsonObject();
                int textIndex = item.get("text_index").getAsInt();
                float[] vector = toFloatArray(item.getAsJsonArray("embedding"));
                indexed.add(new IndexedEmbedding(textIndex, vector));
            }

            indexed.sort(Comparator.comparingInt(IndexedEmbedding::textIndex));
            List<float[]> vectors = new ArrayList<>(indexed.size());
            for (IndexedEmbedding item : indexed) {
                vectors.add(normalize(item.vector()));
            }
            return vectors;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("DashScope Embedding call failed", e);
            throw new RuntimeException("DashScope Embedding 调用异常: " + e.getMessage(), e);
        }
    }

    private float[] toFloatArray(JsonArray array) {
        float[] vector = new float[array.size()];
        for (int i = 0; i < array.size(); i++) {
            vector[i] = array.get(i).getAsFloat();
        }
        return vector;
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

    private record IndexedEmbedding(int textIndex, float[] vector) {}
}
