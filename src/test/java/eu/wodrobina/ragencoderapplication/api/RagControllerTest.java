package eu.wodrobina.ragencoderapplication.api;

import eu.wodrobina.ragencoderapplication.encoder.EmbeddingProvider;
import eu.wodrobina.ragencoderapplication.index.IndexingService;
import eu.wodrobina.ragencoderapplication.index.SearchResult;
import eu.wodrobina.ragencoderapplication.index.VectorStore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = "spring.main.allow-bean-definition-overriding=true")
class RagControllerTest {

    @Autowired
    private RagController ragController;

    private org.springframework.test.web.servlet.MockMvc mockMvc;

    @Autowired
    private EmbeddingProvider embeddingProvider;

    @Autowired
    private IndexingService indexingService;

    @Autowired
    private VectorStore vectorStore;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        mockMvc = org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(ragController).build();
    }

    @Test
    void shouldReturnEmbeddingWhenTextIsProvided() throws Exception {
        when(embeddingProvider.modelName()).thenReturn("test-model");
        when(embeddingProvider.embedQuery(anyString())).thenReturn(List.of(0.1f, 0.2f, 0.3f));

        mockMvc.perform(post("/rag/embed")
                        .contentType("application/json")
                        .content("{\"text\": \"hello\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.model").value("test-model"))
                .andExpect(jsonPath("$.embedding[0]").value(0.1f));
    }

    @Test
    void shouldIndexTextAndReturnChunkCount() throws Exception {
        when(indexingService.indexText(anyString(), anyString(), anyMap(), anyString())).thenReturn(5);

        mockMvc.perform(post("/rag/index-text")
                        .contentType("application/json")
                        .content("{\"sourceId\": \"doc1\", \"text\": \"some text\", \"metadata\": {}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chunksIndexed").value(5));
    }

    @Test
    void shouldReturnSearchResultsWhenQueryIsSent() throws Exception {
        when(embeddingProvider.embedQuery(anyString())).thenReturn(List.of(0.1f, 0.2f, 0.3f));
        when(vectorStore.search(anyList(), anyInt(), anyMap(), anyString())).thenReturn(List.of(new SearchResult("id", "content", 1.0, Map.of())));

        mockMvc.perform(post("/rag/search")
                        .contentType("application/json")
                        .content("{\"query\": \"test\", \"limit\": 10}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("0.content").value("content"));
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        public EmbeddingProvider embeddingProvider() {
            return Mockito.mock(EmbeddingProvider.class);
        }

        @Bean
        public IndexingService indexingService() {
            return Mockito.mock(IndexingService.class);
        }

        @Bean
        public VectorStore vectorStore() {
            return Mockito.mock(VectorStore.class);
        }
    }
}
