package eu.wodrobina.ragencoderapplication.index;

import eu.wodrobina.ragencoderapplication.chunking.Chunk;
import eu.wodrobina.ragencoderapplication.encoder.EmbeddingProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@SpringBootTest
class IndexingServiceIT {

    @Autowired
    private IndexingService indexingService;

    @Autowired
    private VectorStore vectorStore;

    @Autowired
    private EmbeddingProvider embeddingProvider;

    @Test
    void testIndexTextFlow() {
        String sourceId = "test-doc";
        String text = "This is a test sentence for integration testing.";
        Map<String, Object> metadata = Map.of("author", "claude");

        // Mock the embedding provider
        List<Float> dummyVector = List.of(0.1f, 0.2f, 0.3f);
        when(embeddingProvider.embedDocuments(anyList())).thenReturn(List.of(dummyVector));
        when(embeddingProvider.modelName()).thenReturn("test-model");
        when(embeddingProvider.dimension()).thenReturn(3);

        int chunksCount = indexingService.indexText(sourceId, text, metadata);

        assertEquals(1, chunksCount, "Should have created 1 chunk");

        // Verify it's in the vector store
        List<SearchResult> results = vectorStore.search(dummyVector, 5, Map.of());
        assertFalse(results.isEmpty(), "Search should return results");
        assertEquals(text, results.get(0).content());
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        public EmbeddingProvider embeddingProvider() {
            return Mockito.mock(EmbeddingProvider.class);
        }
    }
}