package eu.wodrobina.ragencoderapplication.reranking;

import eu.wodrobina.ragencoderapplication.index.InMemoryVectorStore;
import eu.wodrobina.ragencoderapplication.index.IndexingService;
import eu.wodrobina.ragencoderapplication.index.SearchResult;
import eu.wodrobina.ragencoderapplication.index.VectorStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "rag.reranking.enabled=true",
        "rag.reranking.strategy=lexical-overlap",
        "rag.reranking.vector-weight=0.7",
        "rag.reranking.lexical-weight=0.3",
        "rag.reranking.candidate-limit=20"
})
class RankingIntegrationTest {

    @TestConfiguration
    static class TestVectorStoreConfiguration {

        @Bean
        @Primary
        VectorStore inMemoryVectorStore() {
            return new InMemoryVectorStore();
        }
    }

    @Autowired
    private IndexingService indexingService;


    @Test
    @DisplayName("Should perform full search flow with reranking")
    void shouldPerformFullSearchFlowWithReranking() {
        // given
        String query = "How to bake a cake";

        indexingService.indexText(
                "fox-document",
                "The quick brown fox jumps over the lazy dog",
                Map.of(),
                "documents"
        );
        indexingService.indexText(
                "car-document",
                "A fast red car is driving today",
                Map.of(),
                "documents"
        );
        indexingService.indexText(
                "cake-document",
                "How to bake a cake",
                Map.of(),
                "documents"
        );

        // when
        List<SearchResult> results = indexingService.search(query, 10, Map.of(), "documents");

        // then
        assertFalse(results.isEmpty());
        assertEquals("How to bake a cake", results.getFirst().content());
        assertEquals(3, results.size());
    }
}
