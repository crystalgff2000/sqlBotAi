package com.sqlbot.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sqlbot.config.DeepSeekProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class DeepSeekService {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekService.class);

    private final DeepSeekProperties properties;
    private final Gson gson = new Gson();
    private final HttpClient httpClient;

    public DeepSeekService(DeepSeekProperties properties) {
        this.properties = properties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(properties.getConnectTimeoutSeconds()))
                .build();
    }

    public String chat(String question) {
        return chatInternal(
                "你是 AI 数据资产平台助手，主要擅长企业数据资产、数据治理、指标口径与业务数据分析。"
                        + "若问题属于数据资产/Wiki 知识库范围，请优先基于专业口径回答；"
                        + "若问题属于通用常识（如天气、新闻、编程等），可以用通用知识直接回答，"
                        + "但需说明无法提供实时数据时应明确告知并给出查询建议。"
                        + "请用简洁、准确的中文回答。",
                question,
                0.7
        );
    }

    public String chatWithWikiKnowledge(String question, String context) {
        String systemPrompt = """
                你是 AI 数据资产平台助手。用户问题必须仅依据下方【本地 Wiki 知识库上下文】回答。
                要求：
                1. 只使用上下文中明确出现的事实、公式、字段、规则和定义，不得编造；
                2. 充分整合上下文，给出完整、准确的中文回答；
                3. 回答末尾列出引用来源路径（wiki/...）；
                4. 若上下文确实无法回答问题，仅回复：「本地 Wiki 知识库中未找到该问题的相关内容。」，不要补充外部知识。
                """;
        String userPrompt = "【本地 Wiki 知识库上下文】\n" + context + "\n\n【用户问题】\n" + question;
        return chatInternal(systemPrompt, userPrompt, 0.3);
    }

    public String chatWithContext(String question, String context) {
        String systemPrompt = """
                你是 AI 数据资产平台助手，擅长企业数据资产、数据治理、指标口径与业务数据分析。
                请基于提供的【知识库上下文】回答用户问题，要求：
                1. 若上下文与问题高度相关，优先使用上下文中的事实、公式、字段和规则；
                2. 若上下文与问题明显无关（如用户问天气但上下文是 SQL 脚本），不要强行用上下文，\
                应明确说明知识库无相关内容，并简要给出通用建议；
                3. 不要编造 Wiki 中不存在的数据资产口径；
                4. 使用简洁、准确的中文，相关时末尾列出引用来源路径（wiki/...）。
                """;
        String userPrompt = "【知识库上下文】\n" + context + "\n\n【用户问题】\n" + question;
        return chatInternal(systemPrompt, userPrompt, 0.7);
    }

    private String chatInternal(String systemPrompt, String userPrompt, double temperature) {

        if (!properties.isEnabled()) {
            throw new IllegalStateException("DeepSeek API 未启用，请在配置中设置 deepseek.enabled=true");
        }
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new IllegalStateException("未配置 DeepSeek API Key，请设置环境变量 DEEPSEEK_API_KEY 或 application.yml 中的 deepseek.api-key");
        }

        String url = properties.getBaseUrl().replaceAll("/$", "") + "/v1/chat/completions";

        JsonObject systemMessage = new JsonObject();
        systemMessage.addProperty("role", "system");
        systemMessage.addProperty("content", systemPrompt);

        JsonObject userMessage = new JsonObject();
        userMessage.addProperty("role", "user");
        userMessage.addProperty("content", userPrompt);

        JsonArray messages = new JsonArray();
        messages.add(systemMessage);
        messages.add(userMessage);

        JsonObject body = new JsonObject();
        body.addProperty("model", properties.getModel());
        body.add("messages", messages);
        body.addProperty("stream", false);
        body.addProperty("temperature", temperature);

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
                log.error("DeepSeek API error: status={}, body={}", response.statusCode(), responseBody);
                throw new RuntimeException("DeepSeek API 调用失败 (HTTP " + response.statusCode() + ")");
            }

            JsonObject json = gson.fromJson(responseBody, JsonObject.class);
            if (json.has("error")) {
                String msg = json.getAsJsonObject("error").get("message").getAsString();
                throw new RuntimeException("DeepSeek API 错误: " + msg);
            }

            JsonArray choices = json.getAsJsonArray("choices");
            if (choices == null || choices.isEmpty()) {
                throw new RuntimeException("DeepSeek API 返回空结果");
            }

            return choices.get(0).getAsJsonObject()
                    .getAsJsonObject("message")
                    .get("content").getAsString();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("DeepSeek API call failed", e);
            throw new RuntimeException("DeepSeek API 调用异常: " + e.getMessage(), e);
        }
    }
}
