package com.sqlbot.service.query;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sqlbot.config.QueryBusinessDataProperties;
import com.sqlbot.dto.QueryResultDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class MySqlQueryExecutor {

    private static final Logger log = LoggerFactory.getLogger(MySqlQueryExecutor.class);

    private final QueryBusinessDataProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MySqlQueryExecutor(QueryBusinessDataProperties properties) {
        this.properties = properties;
    }

    public QueryResultDTO execute(String sql, Path outputDir) {
        QueryResultDTO result = new QueryResultDTO();
        try {
            Files.createDirectories(outputDir);
            Path sqlFile = outputDir.resolve("query.sql");
            Files.writeString(sqlFile, sql.strip());

            ProcessBuilder builder = new ProcessBuilder(buildCommandArgs(sqlFile, outputDir));
            builder.redirectErrorStream(true);

            Process process = builder.start();
            String output = new String(process.getInputStream().readAllBytes());
            boolean finished = process.waitFor(120, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                result.setError("MySQL 查询超时");
                return result;
            }

            if (process.exitValue() != 0) {
                result.setError(formatExecutorError(parseExecutorError(output)));
                return result;
            }

            Path resultJson = outputDir.resolve("result.json");
            if (!Files.exists(resultJson)) {
                result.setError("查询执行成功但未生成 result.json");
                return result;
            }

            JsonNode payload = objectMapper.readTree(resultJson.toFile());
            result.setExecutedSql(payload.path("executed_sql").asText(""));
            result.setTruncated(payload.path("truncated").asBoolean(false));
            result.setNotice(payload.path("notice").asText(null));
            result.setRowCount(payload.path("row_count").asInt(0));
            result.setElapsedMs(payload.path("elapsed_ms").asLong(0));

            List<String> columns = new ArrayList<>();
            payload.path("columns").forEach(node -> columns.add(node.asText()));
            result.setColumns(columns);

            List<List<String>> rows = new ArrayList<>();
            payload.path("rows").forEach(rowNode -> {
                List<String> row = new ArrayList<>();
                rowNode.forEach(cell -> row.add(cell.isNull() ? null : cell.asText()));
                rows.add(row);
            });
            result.setRows(rows);
            return result;
        } catch (Exception e) {
            log.error("MySQL query execution failed", e);
            result.setError("查询执行失败: " + e.getMessage());
            return result;
        }
    }

    private List<String> buildCommandArgs(Path sqlFile, Path outputDir) throws IOException {
        List<String> command = new ArrayList<>();
        command.add(properties.getPythonPath());
        command.add(resolveQueryScript().toString());
        command.add("--sql-file");
        command.add(sqlFile.toAbsolutePath().toString());
        command.add("--output-dir");
        command.add(outputDir.toAbsolutePath().toString());
        command.add("--host");
        command.add(properties.getSshHost());
        command.add("--ssh-user");
        command.add(properties.getSshUser());
        command.add("--database");
        command.add(properties.getDatabase());
        command.add("--mysql-config");
        command.add(properties.getMysqlConfig());
        command.add("--max-rows");
        command.add(String.valueOf(properties.getMaxRows()));
        command.add("--timeout-ms");
        command.add(String.valueOf(properties.getTimeoutMs()));

        if ("local".equalsIgnoreCase(properties.getConnectionMode())) {
            command.add("--local");
        }

        String identityFile = resolveIdentityFile();
        if (identityFile != null) {
            command.add("--ssh-identity-file");
            command.add(identityFile);
        }
        return command;
    }

    private String resolveIdentityFile() {
        String configured = properties.getSshIdentityFile();
        if (configured == null || configured.isBlank()) {
            return null;
        }
        String expanded = configured.replace("~", System.getProperty("user.home")).trim();
        Path path = Path.of(expanded);
        if (!Files.exists(path)) {
            throw new IllegalStateException("SSH 私钥文件不存在: " + expanded);
        }
        return path.toAbsolutePath().toString();
    }

    private String formatExecutorError(String message) {
        if (message != null && message.contains("Permission denied")) {
            return message + "。请在本机配置 SSH 免密登录："
                    + "1) 生成密钥 ssh-keygen -t ed25519；"
                    + "2) 上传公钥 ssh-copy-id " + properties.getSshUser() + "@" + properties.getSshHost() + "；"
                    + "3) 或在 application.yml 的 query-business-data.ssh-identity-file 指定私钥路径。";
        }
        return message;
    }

    private String parseExecutorError(String output) {
        try {
            JsonNode node = objectMapper.readTree(output.trim());
            if (node.has("error")) {
                return node.get("error").asText();
            }
        } catch (Exception ignored) {
            // fall through
        }
        return output.isBlank() ? "MySQL 查询执行失败" : output.trim();
    }

    private Path resolveQueryScript() throws IOException {
        ClassPathResource resource = new ClassPathResource(
                "skills/query-business-data/scripts/query_mysql.py");
        Path temp = Files.createTempFile("sqlbot-query-mysql-", ".py");
        try (InputStream inputStream = resource.getInputStream()) {
            Files.copy(inputStream, temp, StandardCopyOption.REPLACE_EXISTING);
        }
        temp.toFile().setExecutable(true);
        return temp;
    }
}
