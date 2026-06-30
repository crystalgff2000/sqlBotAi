package com.sqlbot.service.rag;

import java.util.List;

public interface EmbeddingService {

    enum TextType {
        QUERY,
        DOCUMENT
    }

    float[] embed(String text, TextType textType);

    List<float[]> embedBatch(List<String> texts, TextType textType);

    boolean isAvailable();
}
