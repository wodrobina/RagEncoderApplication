package eu.wodrobina.ragencoderapplication.index;

import eu.wodrobina.ragencoderapplication.chunking.AbstractChunker;
import eu.wodrobina.ragencoderapplication.chunking.Chunk;
import eu.wodrobina.ragencoderapplication.encoder.EmbeddingProvider;
import eu.wodrobina.ragencoderapplication.reranking.Reranker;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class IndexingService {

    private final AbstractChunker chunker;
    private final EmbeddingProvider embeddingProvider;
    private final VectorStore vectorStore;
    private final Reranker reranker;

    public IndexingService(
            AbstractChunker chunker,
            EmbeddingProvider embeddingProvider,
            VectorStore vectorStore,
            Reranker reranker
    ) {
        this.chunker = chunker;
        this.embeddingProvider = embeddingProvider;
        this.vectorStore = vectorStore;
        this.reranker = reranker;
    }

    public int indexText(String sourceId, String text, Map<String, Object> metadata, String collection) {
        List<Chunk> chunks = chunker.chunk(sourceId, text, enrichedMetadata(metadata));

        if (chunks.isEmpty()) {
            return 0;
        }

        List<String> contentsForEmbedding = chunks.stream()
                .map(this::buildEmbeddingText)
                .toList();

        List<List<Float>> embeddings = embeddingProvider.embedDocuments(contentsForEmbedding);

        if (embeddings.size() != chunks.size()) {
            throw new IllegalStateException(
                    "Embedding count does not match chunk count: expected "
                            + chunks.size()
                            + ", got "
                            + embeddings.size()
            );
        }

        List<VectorDocument> documents = new ArrayList<>();

        for (int i = 0; i < chunks.size(); i++) {
            Chunk chunk = chunks.get(i);

            String fileName = resolveFileName(
                    chunk.fileName(),
                    chunk.sourceId(),
                    chunk.metadata()
            );

            String fileType = resolveFileType(
                    chunk.fileType(),
                    fileName
            );

            Map<String, Object> documentMetadata = enrichedDocumentMetadata(
                    chunk.metadata(),
                    chunk.sourceId(),
                    chunk.chunkIndex(),
                    fileName,
                    fileType
            );

            documents.add(new VectorDocument(
                    chunk.id(),
                    chunk.content(),
                    embeddings.get(i),
                    chunk.sourceId(),
                    chunk.chunkIndex(),
                    chunk.contentHash(),
                    chunk.documentHash(),
                    fileName,
                    fileType,
                    Instant.now(),
                    documentMetadata
            ));
        }

        vectorStore.upsert(documents, collection);
        return documents.size();
    }

    public List<SearchResult> search(String query, int limit, Map<String, Object> filter, String collection) {
        List<Float> queryEmbedding = embeddingProvider.embedQuery(query);

        int finalLimit = limit <= 0 ? 10 : limit;
        int candidateLimit = Math.max(finalLimit * 5, 50);

        List<SearchResult> candidates = vectorStore.search(
                queryEmbedding,
                candidateLimit,
                filter == null ? Map.of() : filter,
                collection == null ? "documents" : collection
        );

        return reranker.rerank(query, candidates, finalLimit);
    }

    public List<SearchResult> rerankSearch(String query, List<SearchResult> candidates, int limit) {
        return reranker.rerank(query, candidates, limit);
    }

    private String buildEmbeddingText(Chunk chunk) {
        String fileName = resolveFileName(
                chunk.fileName(),
                chunk.sourceId(),
                chunk.metadata()
        );

        String fileType = resolveFileType(
                chunk.fileType(),
                fileName
        );

        return """
                file: %s
                type: %s
                source: %s
                chunk: %d

                %s
                """.formatted(
                fileName,
                fileType,
                chunk.sourceId(),
                chunk.chunkIndex(),
                chunk.content()
        );
    }

    private Map<String, Object> enrichedMetadata(Map<String, Object> metadata) {
        var copy = metadata == null
                ? new HashMap<String, Object>()
                : new HashMap<>(metadata);

        copy.put("embedding_model", embeddingProvider.modelName());
        copy.put("embedding_dimension", embeddingProvider.dimension());

        return Map.copyOf(copy);
    }

    private Map<String, Object> enrichedDocumentMetadata(
            Map<String, Object> metadata,
            String sourceId,
            int chunkIndex,
            String fileName,
            String fileType
    ) {
        var copy = metadata == null
                ? new HashMap<String, Object>()
                : new HashMap<>(metadata);

        copy.put("sourceId", sourceId);
        copy.put("chunkIndex", chunkIndex);
        copy.put("fileName", fileName);
        copy.put("fileType", fileType);
        copy.put("embedding_model", embeddingProvider.modelName());
        copy.put("embedding_dimension", embeddingProvider.dimension());

        return Map.copyOf(copy);
    }

    private String resolveFileName(String fileName, String sourceId, Map<String, Object> metadata) {
        if (fileName != null && !fileName.isBlank() && !"unknown".equalsIgnoreCase(fileName)) {
            return fileName;
        }

        if (metadata != null && metadata.containsKey("file_path")) {
            Object pathObj = metadata.get("file_path");

            if (pathObj instanceof String pathStr && !pathStr.isBlank()) {
                try {
                    Path path = Path.of(pathStr);
                    Path resolvedFileName = path.getFileName();

                    if (resolvedFileName != null) {
                        return resolvedFileName.toString();
                    }
                } catch (Exception ignored) {
                    // fallback below
                }
            }
        }

        if (metadata != null && metadata.containsKey("fileName")) {
            Object fileNameObj = metadata.get("fileName");

            if (fileNameObj instanceof String metadataFileName && !metadataFileName.isBlank()) {
                return metadataFileName;
            }
        }

        if (metadata != null && metadata.containsKey("path")) {
            Object pathObj = metadata.get("path");

            if (pathObj instanceof String pathStr && !pathStr.isBlank()) {
                try {
                    Path path = Path.of(pathStr);
                    Path resolvedFileName = path.getFileName();

                    if (resolvedFileName != null) {
                        return resolvedFileName.toString();
                    }
                } catch (Exception ignored) {
                    // fallback below
                }
            }
        }

        if (sourceId != null && !sourceId.isBlank()) {
            try {
                Path path = Path.of(sourceId);
                Path resolvedFileName = path.getFileName();

                if (resolvedFileName != null) {
                    return resolvedFileName.toString();
                }
            } catch (Exception ignored) {
                return sourceId;
            }
        }

        return "unknown";
    }

    private String resolveFileType(String fileType, String fileName) {
        if (fileType != null && !fileType.isBlank() && !"unknown".equalsIgnoreCase(fileType)) {
            return fileType;
        }

        if (fileName == null || fileName.isBlank() || "unknown".equalsIgnoreCase(fileName)) {
            return "unknown";
        }

        int dotIndex = fileName.lastIndexOf('.');

        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return "unknown";
        }

        return fileName.substring(dotIndex + 1).toLowerCase();
    }
}