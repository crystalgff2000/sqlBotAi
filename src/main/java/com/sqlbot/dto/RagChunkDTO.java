package com.sqlbot.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RagChunkDTO {
    private String chunkId;
    private String sourcePath;
    private String title;
    private String content;
    private double score;
}
