package com.sqlbot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "rag")
public class RagProperties {

    private boolean enabled = true;
    /** tfidf | dashscope */
    private String vectorStore = "dashscope";
    private int chunkSize = 600;
    private int chunkOverlap = 80;
    private int topK = 5;
    /** 向量检索最低相似度（DashScope cosine） */
    private double minScore = 0.45;
    /** 向量检索达到此分数视为 Wiki 高置信命中（DashScope） */
    private double highConfidenceScore = 0.55;
    /** TF-IDF 回退时的最低分数 */
    private double tfidfMinScore = 0.08;
    /** TF-IDF 回退时的高置信分数 */
    private double tfidfHighConfidenceScore = 0.18;
    private boolean rebuildOnStartup = true;
    private int maxContextLength = 4000;
    /** Embedding 向量缓存目录 */
    private String cacheDir = "data/rag-cache";
}
