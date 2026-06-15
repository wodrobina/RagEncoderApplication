package eu.wodrobina.ragencoderapplication.reranking;

import eu.wodrobina.ragencoderapplication.index.SearchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NoOpRerankerTest {

    private NoOpReranker reranker;

    @BeforeEach
    void setUp() {
        reranker = new NoOpReranker();
    }

    @Test
    @DisplayName("Should maintain the same order and respect the limit")
    void shouldMaintainOrderAndRespectLimit() {
        List<SearchResult> candidates = List.of(
                new SearchResult("1", "content 1", 0.9f, Map.of()),
                new SearchResult("2", "content 2", 0.8f, Map.of()),
                new SearchResult("3", "content 3", 0.7f, Map.of())
        );

        List<SearchResult> result = reranker.rerank("query", candidates, 2);

        assertEquals(2, result.size());
        assertEquals("content 1", result.get(0).content());
        assertEquals("content 2", result.get(1).content());
    }

    @Test
    @DisplayName("Should handle empty candidates")
    void shouldHandleEmptyCandidates() {
        List<SearchResult> candidates = List.of();

        List<SearchResult> result = reranker.rerank("query", candidates, 5);

        assertTrue(result.isEmpty());
    }
}
