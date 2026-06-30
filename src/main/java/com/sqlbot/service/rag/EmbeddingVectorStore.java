package com.sqlbot.service.rag;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.sqlbot.config.DashScopeProperties;
import com.sqlbot.config.RagProperties;
import com.sqlbot.dto.RagChunkDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;

@Component
public class EmbeddingVectorStore {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingVectorStore.class);
    private static final Gson GSON = new GsonBuilder().create();
    private static final Type CACHE_TYPE = new TypeToken<RagEmbeddingCache>() {}.getType();

    private final RagProperties ragProperties;
    private final DashScopeProperties dashScopeProperties;
    private final EmbeddingService embeddingService;

    private final List<IndexedChunk> chunks = new ArrayList<>();

    public EmbeddingVectorStore(
            RagProperties ragProperties,
            DashScopeProperties dashScopeProperties,
            EmbeddingService embeddingService) {
        this.ragProperties = ragProperties;
        this.dashScopeProperties = dashScopeProperties;
        this.embeddingService = embeddingService;
    }

    public synchronized void buildIndex(List<RagChunkDTO> sourceChunks) {
        chunks.clear();
        if (sourceChunks.isEmpty()) {
            return;
        }
        if (!embeddingService.isAvailable()) {
            throw new IllegalStateException("DashScope Embedding 不可用，请配置 dashscope.api-key");
        }

        String indexHash = computeIndexHash(sourceChunks);
        RagEmbeddingCache cache = loadCache(indexHash);
        if (cache != null) {
            for (CachedChunk cached : cache.chunks()) {
                chunks.add(new IndexedChunk(cached.chunk(), cached.vector()));
            }
            log.info("Loaded {} embedding vectors from cache", chunks.size());
            return;
        }

        log.info("Building DashScope embedding index for {} chunks...", sourceChunks.size());
        List<String> texts = sourceChunks.stream().map(RagChunkDTO::getContent).toList();
        List<float[]> vectors = embeddingService.embedBatch(texts, EmbeddingService.TextType.DOCUMENT);

        if (vectors.size() != sourceChunks.size()) {
            throw new IllegalStateException("Embedding 数量与 chunk 数量不一致: "
                    + vectors.size() + " vs " + sourceChunks.size());
        }

        List<CachedChunk> cachedChunks = new ArrayList<>(sourceChunks.size());
        for (int i = 0; i < sourceChunks.size(); i++) {
            RagChunkDTO source = sourceChunks.get(i);
            float[] vector = vectors.get(i);
            chunks.add(new IndexedChunk(source, vector));
            cachedChunks.add(new CachedChunk(source, vector));
        }

        saveCache(new RagEmbeddingCache(
                indexHash,
                dashScopeProperties.getEmbeddingModel(),
                cachedChunks
        ));
        log.info("DashScope embedding index built: {} vectors", chunks.size());
    }

    public List<RagChunkDTO> search(String query, int topK, double minScore) {
        if (chunks.isEmpty()) {
            return List.of();
        }

        float[] queryVector = embeddingService.embed(query, EmbeddingService.TextType.QUERY);
        List<RagChunkDTO> results = new ArrayList<>();

        for (IndexedChunk chunk : chunks) {
            double score = cosineSimilarity(queryVector, chunk.vector());
            if (score >= minScore) {
                results.add(new RagChunkDTO(
                        chunk.source().getChunkId(),
                        chunk.source().getSourcePath(),
                        chunk.source().getTitle(),
                        chunk.source().getContent(),
                        score
                ));
            }
        }

        results.sort(Comparator.comparingDouble(RagChunkDTO::getScore).reversed());
        if (results.size() > topK) {
            return results.subList(0, topK);
        }
        return results;
    }

    public int size() {
        return chunks.size();
    }

    public boolean isReady() {
        return !chunks.isEmpty();
    }

    private double cosineSimilarity(float[] left, float[] right) {
        if (left.length != right.length) {
            return 0.0;
        }
        double dot = 0.0;
        for (int i = 0; i < left.length; i++) {
            dot += left[i] * right[i];
        }
        return dot;
    }

    private RagEmbeddingCache loadCache(String indexHash) {
        Path cacheFile = cacheFilePath();
        if (!Files.exists(cacheFile)) {
            return null;
        }
        try {
            String json = Files.readString(cacheFile, StandardCharsets.UTF_8);
            RagEmbeddingCache cache = GSON.fromJson(json, CACHE_TYPE);
            if (cache == null) {
                return null;
            }
            if (!indexHash.equals(cache.indexHash())) {
                log.info("Embedding cache hash mismatch, rebuilding index");
                return null;
            }
            if (!dashScopeProperties.getEmbeddingModel().equals(cache.model())) {
                log.info("Embedding model changed, rebuilding index");
                return null;
            }
            return cache;
        } catch (Exception e) {
            log.warn("Failed to load embedding cache, rebuilding index", e);
            return null;
        }
    }

    private void saveCache(RagEmbeddingCache cache) {
        try {
            Path cacheFile = cacheFilePath();
            Files.createDirectories(cacheFile.getParent());
            Files.writeString(cacheFile, GSON.toJson(cache), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.warn("Failed to save embedding cache", e);
        }
    }

    private Path cacheFilePath() {
        return Path.of(ragProperties.getCacheDir(), "embedding-index.json");
    }

    private String computeIndexHash(List<RagChunkDTO> sourceChunks) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(dashScopeProperties.getEmbeddingModel().getBytes(StandardCharsets.UTF_8));
            for (RagChunkDTO chunk : sourceChunks) {
                digest.update(chunk.getChunkId().getBytes(StandardCharsets.UTF_8));
                digest.update(chunk.getContent().getBytes(StandardCharsets.UTF_8));
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute index hash", e);
        }
    }

    private record IndexedChunk(RagChunkDTO source, float[] vector) {}

    private record CachedChunk(RagChunkDTO chunk, float[] vector) {}

    private record RagEmbeddingCache(String indexHash, String model, List<CachedChunk> chunks) {}
}
