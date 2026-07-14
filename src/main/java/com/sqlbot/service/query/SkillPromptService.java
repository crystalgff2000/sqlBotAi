package com.sqlbot.service.query;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
public class SkillPromptService {

    public String loadQuerySkillPrompt() {
        return loadResource(
                "skills/query-business-data/SKILL.md",
                "skills/query-business-data/references/mysql-dialect.md",
                "skills/query-business-data/references/mvp-questions.md"
        );
    }

    private String loadResource(String... paths) {
        StringBuilder builder = new StringBuilder();
        for (String path : paths) {
            try {
                ClassPathResource resource = new ClassPathResource(path);
                if (!resource.exists()) {
                    continue;
                }
                builder.append(resource.getContentAsString(StandardCharsets.UTF_8)).append("\n\n");
            } catch (IOException ignored) {
                // skip missing reference files
            }
        }
        return builder.toString().trim();
    }
}
