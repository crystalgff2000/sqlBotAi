package com.sqlbot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "query-business-data")
public class QueryBusinessDataProperties {

    private boolean enabled = true;
    private String pythonPath = "python3";
    private String outputDir = "data/query-results";
    private String sshHost = "120.48.17.175";
    private String sshUser = "root";
    private String connectionMode = "ssh";
    /** SSH 私钥路径，例如 ~/.ssh/id_ed25519；未配置时使用系统默认密钥 */
    private String sshIdentityFile = "";
    private String database = "ADS";
    private String mysqlConfig = "/root/.ai-data-query.cnf";
    private int maxRows = 500;
    private int timeoutMs = 30000;
}
