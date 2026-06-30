package com.sqlbot.service.rag;

import com.sqlbot.dto.RagChunkDTO;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TfIdfVectorStore {

    @Getter
    private final List<IndexedChunk> chunks = new ArrayList<>();
    private final Map<String, Integer> documentFrequency = new HashMap<>();
    private int totalDocuments = 0;

    public void buildIndex(List<RagChunkDTO> sourceChunks) {
        chunks.clear();
        documentFrequency.clear();
        totalDocuments = sourceChunks.size();

        List<Map<String, Double>> tfVectors = new ArrayList<>();
        for (RagChunkDTO chunk : sourceChunks) {
            List<String> tokens = TextTokenizer.tokenize(chunk.getTitle() + " " + chunk.getContent());
            Map<String, Double> tf = termFrequency(tokens);
            tfVectors.add(tf);
            for (String term : tf.keySet()) {
                documentFrequency.merge(term, 1, Integer::sum);
            }
        }

        for (int i = 0; i < sourceChunks.size(); i++) {
            RagChunkDTO source = sourceChunks.get(i);
            Map<String, Double> tfidf = toTfIdf(tfVectors.get(i));
            chunks.add(new IndexedChunk(source, tfidf));
        }
    }

    public List<RagChunkDTO> search(String query, int topK, double minScore) {
        if (chunks.isEmpty()) {
            return List.of();
        }

        Map<String, Double> queryVector = toTfIdf(termFrequency(TextTokenizer.tokenize(query)));
        List<RagChunkDTO> results = new ArrayList<>();

        for (IndexedChunk chunk : chunks) {
            double score = cosineSimilarity(queryVector, chunk.tfidfVector());
            if (score >= minScore) {
                RagChunkDTO result = new RagChunkDTO(
                        chunk.source().getChunkId(),
                        chunk.source().getSourcePath(),
                        chunk.source().getTitle(),
                        chunk.source().getContent(),
                        score
                );
                results.add(result);
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

    private Map<String, Double> termFrequency(List<String> tokens) {
        Map<String, Double> tf = new HashMap<>();
        if (tokens.isEmpty()) {
            return tf;
        }
        for (String token : tokens) {
            tf.merge(token, 1.0, Double::sum);
        }
        for (Map.Entry<String, Double> entry : tf.entrySet()) {
            entry.setValue(entry.getValue() / tokens.size());
        }
        return tf;
    }

    private Map<String, Double> toTfIdf(Map<String, Double> tf) {
        Map<String, Double> tfidf = new HashMap<>();
        for (Map.Entry<String, Double> entry : tf.entrySet()) {
            int df = documentFrequency.getOrDefault(entry.getKey(), 0);
            double idf = Math.log((totalDocuments + 1.0) / (df + 1.0)) + 1.0;
            tfidf.put(entry.getKey(), entry.getValue() * idf);
        }
        return tfidf;
    }

    private double cosineSimilarity(Map<String, Double> left, Map<String, Double> right) {
        double dot = 0.0;
        for (Map.Entry<String, Double> entry : left.entrySet()) {
            Double other = right.get(entry.getKey());
            if (other != null) {
                dot += entry.getValue() * other;
            }
        }
        double normLeft = 0.0;
        for (double value : left.values()) {
            normLeft += value * value;
        }
        double normRight = 0.0;
        for (double value : right.values()) {
            normRight += value * value;
        }
        if (normLeft == 0.0 || normRight == 0.0) {
            return 0.0;
        }
        return dot / (Math.sqrt(normLeft) * Math.sqrt(normRight));
    }

    private record IndexedChunk(RagChunkDTO source, Map<String, Double> tfidfVector) {}
}
