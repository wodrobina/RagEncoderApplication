package eu.wodrobina.ragencoderapplication.reranking;

import eu.wodrobina.ragencoderapplication.index.SearchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LexicalOverlapRerankerTest {

    private LexicalOverlapReranker reranker;

    @BeforeEach
    void setUp() {
        reranker = new LexicalOverlapReranker(0.7, 0.3);
    }

    @Test
    @DisplayName("Should rank results based on token overlap")
    void shouldRankBasedOnOverlap() {
        List<SearchResult> candidates = List.of(
                new SearchResult("1", "The quick brown fox jumps over the lazy dog", 0.8f, Map.of()),
                new SearchResult("2", "A fast red car is driving today", 0.8f, Map.of()),
                new SearchResult("3", "How to bake a cake", 0.8f, Map.of())
        );

        // Query has tokens: [how, to, bake, a, cake]
        // Candidate 1 overlap: 0 (none)
        // Candidate 2 overlap: 0 (none)
        // Candidate 3 overlap: 3 (how, to, bake) -> Score = 3/5 = 0.6

        List<SearchResult> result = reranker.rerank("How to bake a cake", candidates, 10);

        assertEquals(3, result.size());
        assertEquals("How to bake a cake", result.get(0).content());
    }

    @Test
    @DisplayName("Should handle empty candidates")
    void shouldHandleEmptyCandidates() {
        List<SearchResult> results = reranker.rerank("query", List.of(), 5);
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("Should respect limit even if multiple results have overlap")
    void shouldRespectLimit() {
        List<SearchResult> candidates = List.of(
                new SearchResult("1", "test content", 0.8f, Map.of()),
                new SearchResult("2", "test content", 0.8f, Map.of())
        );

        List<SearchResult> result = reranker.rerank("query", candidates, 1);
        assertEquals(1, result.size());
    }
}
