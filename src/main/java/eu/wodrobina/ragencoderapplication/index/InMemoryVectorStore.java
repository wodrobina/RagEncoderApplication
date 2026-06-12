package eu.wodrobina.ragencoderapplication.index;

import java.util.*;

public class InMemoryVectorStore implements VectorStore {

    private final Map<String, List<VectorDocument>> collections = new HashMap<>();

    @Override
    public void upsert(List<VectorDocument> documents, String collection) {
        collections.computeIfAbsent(collection, k -> new ArrayList<>()).addAll(documents);
    }

    @Override
    public List<SearchResult> search(List<Float> queryVector, int limit, Map<String, Object> filter, String collection) {
        List<VectorDocument> docs = collections.getOrDefault(collection, List.of());
        return docs.stream()
                .map(doc -> {
                    double score = cosineSimilarity(queryVector, doc.embedding());
                    return new SearchResult(doc.id(), doc.content(), score, doc.metadata());
                })
                .sorted(Comparator.comparingDouble(SearchResult::score).reversed())
                .limit(limit)
                .toList();
    }

    private double cosineSimilarity(List<Float> v1, List<Float> v2) {
        if (v1.size() != v2.size()) {
            throw new IllegalArgumentException("Vectors must have the same dimension");
        }
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < v1.size(); i++) {
            dotProduct += v1.get(i) * v2.get(i);
            normA += Math.pow(v1.get(i), 2);
            normB += Math.pow(v2.get(i), 2);
        }
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
