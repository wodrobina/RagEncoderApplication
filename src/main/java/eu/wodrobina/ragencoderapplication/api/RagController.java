package eu.wodrobina.ragencoderapplication.api;

import eu.wodrobina.ragencoderapplication.config.FileScannerProperties;
import eu.wodrobina.ragencoderapplication.encoder.EmbeddingProvider;
import eu.wodrobina.ragencoderapplication.index.IndexingService;
import eu.wodrobina.ragencoderapplication.index.SearchResult;
import eu.wodrobina.ragencoderapplication.index.VectorStore;
import eu.wodrobina.ragencoderapplication.scanner.FileScanner;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/rag")
public class RagController {

    private final EmbeddingProvider embeddingProvider;
    private final IndexingService indexingService;
    private final VectorStore vectorStore;
    private final FileScanner fileScanner;

    public RagController(
            EmbeddingProvider embeddingProvider,
            IndexingService indexingService,
            VectorStore vectorStore,
            FileScanner fileScanner
    ) {
        this.embeddingProvider = embeddingProvider;
        this.indexingService = indexingService;
        this.vectorStore = vectorStore;
        this.fileScanner = fileScanner;
    }

    @PostMapping("/embed")
    public EmbedResponse embed(@Valid @RequestBody EmbedRequest request) {
        return new EmbedResponse(
                embeddingProvider.modelName(),
                embeddingProvider.embedQuery(request.text())
        );
    }

    @PostMapping("/index-text")
    public IndexResponse indexText(@Valid @RequestBody IndexTextRequest request) {
        int chunks = indexingService.indexText(
                request.sourceId(),
                request.text(),
                request.metadata() == null ? Map.of() : request.metadata(),
                request.collection() == null ? "documents" : request.collection()
        );

        return new IndexResponse(chunks);
    }

    @PostMapping("/index-file")
    public IndexResponse indexFile(@Valid @RequestBody IndexFileRequest request) throws Exception {
        String content = fileScanner.readContent(Paths.get(request.path()));
        int chunks = indexingService.indexText(
                request.sourceId(),
                content,
                Map.of("file_path", request.path()),
                "documents"
        );

        return new IndexResponse(chunks);
    }

    @PostMapping("/index-directory")
    public IndexResponse indexDirectory(@Valid @RequestBody IndexDirectoryRequest request) throws Exception {
        List<Path> files = fileScanner.scan();
        int totalChunks = 0;

        for (Path path : files) {
            String content = fileScanner.readContent(path);
            totalChunks += indexingService.indexText(
                    path.toString(),
                    content,
                    Map.of("file_path", path.toString()),
                    "documents"
            );
        }

        return new IndexResponse(totalChunks);
    }

    @PostMapping("/search")
    public List<SearchResult> search(@Valid @RequestBody SearchRequest request) {
        List<Float> queryEmbedding = embeddingProvider.embedQuery(request.query());

        return vectorStore.search(
                queryEmbedding,
                request.limit() <= 0 ? 10 : request.limit(),
                request.filter() == null ? Map.of() : request.filter(),
                request.collection() == null ? "documents" : request.collection()
        );
    }

    @GetMapping("/health")
    public HealthResponse healthCheck() {
        boolean qdrantUp = checkStatus("http://192.168.87.80:6333/health");
        boolean ollamaUp = checkStatus("http://localhost:11434/api/tags");

        return new HealthResponse(
                "UP",
                qdrantUp ? "UP" : "DOWN",
                ollamaUp ? "UP" : "DOWN"
        );
    }

    @DeleteMapping("/documents/{sourceId}")
    public void deleteDocuments(@PathVariable String sourceId) {
        System.out.println("Deletion requested for sourceId: " + sourceId);
    }

    private boolean checkStatus(String url) {
        try {
            HttpClient client = HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(2)).build();
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    public record EmbedRequest(
            @NotBlank(message = "Text is required") String text
    ) {}

    public record EmbedResponse(String model, List<Float> embedding) {}

    public record IndexTextRequest(
            @NotBlank(message = "Source ID is required") String sourceId,
            @NotBlank(message = "Text is required") String text,
            Map<String, Object> metadata,
            String collection
    ) {}

    public record IndexResponse(int chunksIndexed) {}

    public record SearchRequest(
            @NotBlank(message = "Query is required") String query,
            @Min(1) @Max(50) int limit,
            Map<String, Object> filter,
            String collection
    ) {}

    public record IndexFileRequest(
            @NotBlank(message = "Path is required") String path,
            @NotBlank(message = "Source ID is required") String sourceId
    ) {}

    public record IndexDirectoryRequest(
            @NotBlank(message = "Path is required") String path
    ) {}

    public record HealthResponse(
            String status,
            String qdrant,
            String ollama
    ) {}
}
