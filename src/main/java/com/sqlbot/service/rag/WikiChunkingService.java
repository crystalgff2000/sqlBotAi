package com.sqlbot.service.rag;

import com.sqlbot.config.RagProperties;
import com.sqlbot.dto.RagChunkDTO;
import com.sqlbot.dto.WikiPageDTO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class WikiChunkingService {

    private final RagProperties ragProperties;

    public WikiChunkingService(RagProperties ragProperties) {
        this.ragProperties = ragProperties;
    }

    public List<RagChunkDTO> chunkPages(List<WikiPageDTO> pages) {
        List<RagChunkDTO> chunks = new ArrayList<>();
        for (WikiPageDTO page : pages) {
            chunks.addAll(chunkPage(page));
        }
        return chunks;
    }

    private List<RagChunkDTO> chunkPage(WikiPageDTO page) {
        List<RagChunkDTO> chunks = new ArrayList<>();
        String body = stripFrontmatter(page.getContent());
        String[] blocks = body.split("\n\n");

        StringBuilder current = new StringBuilder();
        int chunkIndex = 0;

        for (String block : blocks) {
            String trimmed = block.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            if (current.length() + trimmed.length() + 2 > ragProperties.getChunkSize() && !current.isEmpty()) {
                chunks.add(buildChunk(page, chunkIndex++, current.toString()));
                current = new StringBuilder(overlapTail(current.toString()));
            }

            if (!current.isEmpty()) {
                current.append("\n\n");
            }
            current.append(trimmed);
        }

        if (!current.isEmpty()) {
            chunks.add(buildChunk(page, chunkIndex, current.toString()));
        }

        if (chunks.isEmpty()) {
            chunks.add(buildChunk(page, 0, page.getTitle()));
        }
        return chunks;
    }

    private RagChunkDTO buildChunk(WikiPageDTO page, int index, String content) {
        String chunkId = page.getRelativePath() + "#" + index;
        String enriched = "文档: " + page.getTitle() + "\n路径: wiki/" + page.getRelativePath() + "\n\n" + content;
        return new RagChunkDTO(chunkId, page.getRelativePath(), page.getTitle(), enriched, 0.0);
    }

    private String overlapTail(String text) {
        int overlap = ragProperties.getChunkOverlap();
        if (text.length() <= overlap) {
            return text;
        }
        return text.substring(text.length() - overlap);
    }

    private String stripFrontmatter(String content) {
        if (content.startsWith("---")) {
            int end = content.indexOf("---", 3);
            if (end > 0) {
                return content.substring(end + 3);
            }
        }
        return content;
    }
}
