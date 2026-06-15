package eu.wodrobina.ragencoderapplication.reranking;

import eu.wodrobina.ragencoderapplication.index.SearchResult;

import java.util.List;

public class NoOpReranker implements Reranker {

    @Override
    public List<SearchResult> rerank(String query, List<SearchResult> candidates, int limit) {
        if (candidates == null || candidates.isEmpty() || limit <= 0) {
            return List.of();
        }

        return candidates.stream()
                .limit(limit)
                .toList();
    }
}