package eu.wodrobina.ragencoderapplication.index;

import eu.wodrobina.ragencoderapplication.encoder.EmbeddingProvider;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
class IndexingServiceIT {

    @Autowired
    private IndexingService indexingService;

    @Autowired
    private EmbeddingProvider embeddingProvider;

    @MockBean(name = "vectorStore")
    private VectorStore mockVectorStore;

    @Test
    void shouldSuccessfullyIndexTextAndMakeItSearchable() {
        String sourceId = "test-doc";
        String text = "This is a test sentence for integration testing.";
        Map<String, Object> metadata = Map.of("author", "claude");

        List<Float> dummyVector = List.of(0.1f, 0.2f, 0.3f);

        when(embeddingProvider.embedDocuments(anyList()))
                .thenReturn(List.of(dummyVector));
        when(embeddingProvider.modelName())
                .thenReturn("test-model");
        when(embeddingProvider.dimension())
                .thenReturn(3);
        when(mockVectorStore.search(anyList(), anyInt(), anyMap(), anyString()))
                .thenReturn(List.of(new SearchResult("id", text, 1.0, Map.of())));

        int chunksCount = indexingService.indexText(sourceId, text, metadata, "documents");

        assertEquals(1, chunksCount, "Should have created 1 chunk");

        List<SearchResult> results = mockVectorStore.search(dummyVector, 5, Map.of(), "documents");

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