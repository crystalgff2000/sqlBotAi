package com.sqlbot.service;

import com.sqlbot.config.DeepSeekProperties;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DeepSeekService {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekService.class);

    private final DeepSeekProperties properties;
    private final ChatModel chatModel;

    public DeepSeekService(DeepSeekProperties properties, @Autowired(required = false) ChatModel chatModel) {
        this.properties = properties;
        this.chatModel = chatModel;
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
                3. 使用 Markdown 格式输出：首段为**直接答案**；要点用 `-` 列表；字段名、表名用反引号包裹；
                4. 回答末尾单独一段 provenance，每行一个字段，格式为「来源层级：...」「依据页面：...」；
                5. 若上下文确实无法回答问题，仅回复：「本地 Wiki 知识库中未找到该问题的相关内容。」，不要补充外部知识。
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

    public String generateSql(String question, String wikiContext, String skillPrompt) {
        String systemPrompt = skillPrompt + """

                你现在处于 SQL 生成阶段。请严格依据 Wiki 上下文生成一条 MySQL 8.4 只读查询。
                要求：
                1. 只输出一条 SELECT 或 WITH ... SELECT 语句；
                2. 使用 ADS/DIM/CDM 全限定表名；
                3. 不要 SQL 注释，不要多条语句；
                4. 轮动专案与进攻专案视为同一项目；
                5. 省区字段为 lev3_name，大区字段为 lev2_name；禁止使用 province_name、region_name 等臆造字段；
                6. 查询奖励金额时使用 ADS.ads_bi_attack_exemplary_case_final_reward_df 的 final_reward，不要用 estimate_reward；
                7. 大区/省区维度来自 ADS.ads_bi_attack_exemplary_case_target_achieve_df；按省区汇总奖励时，先将目标达成表收敛到 data_date + politics_code + coalesce(shop_code, chainstore_code) 粒度，再关联产品系列粒度的奖励表，避免行膨胀；
                8. 最新一期用 MAX(data_date)；用户指定大区时，用 lev2_name 过滤，例如 lev2_name = '中一区'；
                9. 若信息不足，仍尽量给出最合理的查询，不要输出解释文字。
                """;
        String userPrompt = "【Wiki 知识上下文】\n" + wikiContext + "\n\n【用户问题】\n" + question;
        return chatInternal(systemPrompt, userPrompt, 0.1);
    }

    public String summarizeQueryResult(
            String question,
            String wikiContext,
            String resultJson,
            String executedSql,
            boolean truncated) {
        String systemPrompt = """
                你是 AI 数据资产平台助手。请基于 Wiki 口径和 MySQL 查询结果，用中文给出业务分析。
                要求：
                1. 先给出**直接答案**（首段加粗）；
                2. 再用 2-4 条 `-` 要点总结结论，只能依据返回数据，不得编造；字段名、表名用反引号；
                3. 末尾单独一段 provenance，每行一个字段：来源层级、依据页面、执行表、数据范围、返回行数、置信度；
                4. 若结果被截断，必须明确提示汇总可能不完整。
                5. 全文使用 Markdown 格式，不要使用 HTML。
                """;
        String userPrompt = "【Wiki 知识上下文】\n" + wikiContext
                + "\n\n【用户问题】\n" + question
                + "\n\n【执行 SQL】\n" + executedSql
                + "\n\n【查询结果 JSON】\n" + resultJson
                + "\n\n【是否截断】\n" + truncated;
        return chatInternal(systemPrompt, userPrompt, 0.2);
    }

    private String chatInternal(String systemPrompt, String userPrompt, double temperature) {
        ensureAvailable();

        try {
            ChatRequest request = ChatRequest.builder()
                    .messages(
                            SystemMessage.from(systemPrompt),
                            UserMessage.from(userPrompt)
                    )
                    .temperature(temperature)
                    .build();
            ChatResponse response = chatModel.chat(request);
            if (response == null || response.aiMessage() == null) {
                throw new RuntimeException("DeepSeek API 返回空结果");
            }
            return response.aiMessage().text();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("DeepSeek API call failed", e);
            throw new RuntimeException("DeepSeek API 调用异常: " + e.getMessage(), e);
        }
    }

    private void ensureAvailable() {
        if (!properties.isEnabled()) {
            throw new IllegalStateException("DeepSeek API 未启用，请在配置中设置 deepseek.enabled=true");
        }
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new IllegalStateException(
                    "未配置 DeepSeek API Key，请设置环境变量 DEEPSEEK_API_KEY 或 application.yml 中的 deepseek.api-key");
        }
        if (chatModel == null) {
            throw new IllegalStateException("LangChain4j DeepSeek ChatModel 未初始化，请检查 deepseek 配置");
        }
    }
}
