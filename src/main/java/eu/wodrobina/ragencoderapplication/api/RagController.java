package eu.wodrobina.ragencoderapplication.api;

import eu.wodrobina.ragencoderapplication.encoder.EmbeddingProvider;
import eu.wodrobina.ragencoderapplication.index.IndexingService;
import eu.wodrobina.ragencoderapplication.index.SearchResult;
import eu.wodrobina.ragencoderapplication.index.VectorStore;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/rag")
public class RagController {

    private final EmbeddingProvider embeddingProvider;
    private final IndexingService indexingService;
    private final VectorStore vectorStore;

    public RagController(
            EmbeddingProvider embeddingProvider,
            IndexingService indexingService,
            VectorStore vectorStore
    ) {
        this.embeddingProvider = embeddingProvider;
        this.indexingService = indexingService;
        this.vectorStore = vectorStore;
    }

    @PostMapping("/embed")
    public EmbedResponse embed(@RequestBody EmbedRequest request) {
        return new EmbedResponse(
                embeddingProvider.modelName(),
                embeddingProvider.embedQuery(request.text())
        );
    }

    @PostMapping("/index-text")
    public IndexResponse indexText(@RequestBody IndexTextRequest request) {
        int chunks = indexingService.indexText(
                request.sourceId(),
                request.text(),
                request.metadata() == null ? Map.of() : request.metadata()
        );

        return new IndexResponse(chunks);
    }

    @PostMapping("/search")
    public List<SearchResult> search(@RequestBody SearchRequest request) {
        List<Float> queryEmbedding = embeddingProvider.embedQuery(request.query());

        return vectorStore.search(
                queryEmbedding,
                request.limit() <= 0 ? 10 : request.limit(),
                request.filter() == null ? Map.of() : request.filter()
        );
    }

    public record EmbedRequest(String text) {}

    public record EmbedResponse(String model, List<Float> embedding) {}

    public record IndexTextRequest(
            String sourceId,
            String text,
            Map<String, Object> metadata
    ) {}

    public record IndexResponse(int chunksIndexed) {}

    public record SearchRequest(
            String query,
            int limit,
            Map<String, Object> filter
    ) {}
}