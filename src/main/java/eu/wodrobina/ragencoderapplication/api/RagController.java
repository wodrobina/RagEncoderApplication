package eu.wodrobina.ragencoderapplication.api;

import eu.wodrobina.ragencoderapplication.encoder.EmbeddingProvider;
import eu.wodrobina.ragencoderapplication.index.IndexingService;
import eu.wodrobina.ragencoderapplication.index.SearchResult;
import eu.wodrobina.ragencoderapplication.index.VectorStore;
import eu.wodrobina.ragencoderapplication.config.RerankingProperties;
import eu.wodrobina.ragencoderapplication.reranking.Reranker;
import eu.wodrobina.ragencoderapplication.scanner.FileScanner;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@RestController
@RequestMapping("/rag")
public class RagController {

    private static final Logger log = LoggerFactory.getLogger(RagController.class);

    private static final String DEFAULT_COLLECTION = "documents";
    private static final int DEFAULT_SEARCH_LIMIT = 10;
    private static final int DEFAULT_INDEXING_WORKERS = 4;

    private final EmbeddingProvider embeddingProvider;
    private final IndexingService indexingService;
    private final VectorStore vectorStore;
    private final FileScanner fileScanner;
    private final RerankingProperties rerankingProperties;
    private final Reranker reranker;
    private final HttpClient httpClient;

    public RagController(
            EmbeddingProvider embeddingProvider,
            IndexingService indexingService,
            VectorStore vectorStore,
            FileScanner fileScanner,
            RerankingProperties rerankingProperties,
            Reranker reranker
    ) {
        this.embeddingProvider = embeddingProvider;
        this.indexingService = indexingService;
        this.vectorStore = vectorStore;
        this.fileScanner = fileScanner;
        this.rerankingProperties = rerankingProperties;
        this.reranker = reranker;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
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
                resolveCollection(request.collection())
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
                DEFAULT_COLLECTION
        );

        return new IndexResponse(chunks);
    }

    @PostMapping("/index-directory")
    public IndexResponse indexDirectory(@Valid @RequestBody IndexDirectoryRequest request) throws Exception {
        List<Path> files = fileScanner.scan(Path.of(request.path()));
        String collection = resolveCollection(request.collection());
        int workers = resolveIndexingWorkers(request.parallelism(), files.size());

        log.info(
                "Found {} files to index in directory: {}. Collection: {}. Workers: {}",
                files.size(),
                request.path(),
                collection,
                workers
        );

        if (files.isEmpty()) {
            return new IndexResponse(0);
        }

        int totalChunks = 0;
        int indexedFiles = 0;
        int skippedFiles = 0;

        try (ExecutorService executor = Executors.newFixedThreadPool(workers)) {
            CompletionService<FileIndexResult> completionService = new ExecutorCompletionService<>(executor);

            for (int i = 0; i < files.size(); i++) {
                Path path = files.get(i);
                int fileNumber = i + 1;
                int totalFiles = files.size();

                completionService.submit(() -> indexSingleFile(path, fileNumber, totalFiles, collection));
            }

            for (int i = 0; i < files.size(); i++) {
                Future<FileIndexResult> future = completionService.take();
                FileIndexResult result = future.get();

                if (result.success()) {
                    totalChunks += result.chunks();
                    indexedFiles++;

                    log.info(
                            "Indexed file {}/{}: {} -> {} chunks",
                            result.fileNumber(),
                            result.totalFiles(),
                            result.path(),
                            result.chunks()
                    );
                } else {
                    skippedFiles++;

                    log.warn(
                            "Skipping file {}/{}: {} because: {}",
                            result.fileNumber(),
                            result.totalFiles(),
                            result.path(),
                            result.errorMessage()
                    );
                }
            }
        }

        log.info(
                "Finished indexing directory: {}. Collection: {}. Indexed files: {}. Skipped files: {}. Total chunks: {}",
                request.path(),
                collection,
                indexedFiles,
                skippedFiles,
                totalChunks
        );

        return new IndexResponse(totalChunks);
    }

    @PostMapping("/search")
    public List<SearchResult> search(@Valid @RequestBody SearchRequest request) {
        int limit = resolveSearchLimit(request.limit());
        Map<String, Object> filter = request.filter() == null ? Map.of() : request.filter();
        String collection = resolveCollection(request.collection());

        List<Float> queryEmbedding = embeddingProvider.embedQuery(request.query());

        if (Boolean.TRUE.equals(request.rerank()) && rerankingProperties.isEnabled()) {
            int candidateLimit = request.candidateLimit() == null
                    ? rerankingProperties.getCandidateLimit()
                    : request.candidateLimit();

            candidateLimit = Math.max(candidateLimit, limit);

            List<SearchResult> candidates = vectorStore.search(
                    queryEmbedding,
                    candidateLimit,
                    filter,
                    collection
            );

            return reranker.rerank(request.query(), candidates, limit);
        }

        return vectorStore.search(
                queryEmbedding,
                limit,
                filter,
                collection
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
        log.info("Deletion requested for sourceId: {}", sourceId);

        // TODO: Podłącz usuwanie dokumentów do VectorStore / IndexingService,
        // gdy taka operacja będzie dostępna w warstwie index.
    }

    private FileIndexResult indexSingleFile(Path path, int fileNumber, int totalFiles, String collection) {
        try {
            log.info("Indexing file {}/{}: {}", fileNumber, totalFiles, path);

            String content = fileScanner.readContent(path);

            int chunks = indexingService.indexText(
                    path.toString(),
                    content,
                    Map.of(
                            "file_path", path.toString(),
                            "file_name", path.getFileName().toString()
                    ),
                    collection
            );

            return FileIndexResult.success(path, fileNumber, totalFiles, chunks);
        } catch (Exception e) {
            log.warn("Failed to index file {}/{}: {}", fileNumber, totalFiles, path, e);
            return FileIndexResult.failure(path, fileNumber, totalFiles, e.getMessage());
        }
    }

    private int resolveIndexingWorkers(Integer requestedParallelism, int filesCount) {
        if (filesCount <= 0) {
            return 1;
        }

        int requestedWorkers = requestedParallelism == null || requestedParallelism <= 0
                ? DEFAULT_INDEXING_WORKERS
                : requestedParallelism;

        return Math.max(1, Math.min(requestedWorkers, filesCount));
    }

    private int resolveSearchLimit(Integer requestedLimit) {
        if (requestedLimit == null || requestedLimit <= 0) {
            return DEFAULT_SEARCH_LIMIT;
        }

        return requestedLimit;
    }


    private String resolveCollection(String collection) {
        if (collection == null || collection.isBlank()) {
            return DEFAULT_COLLECTION;
        }

        return collection;
    }

    private boolean checkStatus(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(2))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            return response.statusCode() == 200;
        } catch (Exception e) {
            log.debug("Health check failed for URL: {}", url, e);
            return false;
        }
    }

    private record FileIndexResult(
            Path path,
            int fileNumber,
            int totalFiles,
            int chunks,
            String errorMessage
    ) {

        static FileIndexResult success(Path path, int fileNumber, int totalFiles, int chunks) {
            return new FileIndexResult(path, fileNumber, totalFiles, chunks, null);
        }

        static FileIndexResult failure(Path path, int fileNumber, int totalFiles, String errorMessage) {
            return new FileIndexResult(path, fileNumber, totalFiles, 0, errorMessage);
        }

        boolean success() {
            return errorMessage == null;
        }
    }

    public record EmbedRequest(
            @NotBlank(message = "Text is required")
            String text
    ) {}

    public record EmbedResponse(
            String model,
            List<Float> embedding
    ) {}

    public record IndexTextRequest(
            @NotBlank(message = "Source ID is required")
            String sourceId,

            @NotBlank(message = "Text is required")
            String text,

            Map<String, Object> metadata,

            String collection
    ) {}

    public record IndexResponse(
            int chunksIndexed
    ) {}

    public record SearchRequest(
            @NotBlank(message = "Query is required")
            String query,

            @Min(1)
            @Max(50)
            Integer limit,

            Map<String, Object> filter,

            String collection,

            Boolean rerank,

            @Min(1)
            Integer candidateLimit
    ) {}

    public record IndexFileRequest(
            @NotBlank(message = "Path is required")
            String path,

            @NotBlank(message = "Source ID is required")
            String sourceId
    ) {}

    public record IndexDirectoryRequest(
            @NotBlank(message = "Path is required")
            String path,

            String collection,

            @Min(1)
            @Max(32)
            Integer parallelism
    ) {}

    public record HealthResponse(
            String status,
            String qdrant,
            String ollama
    ) {}
}