package eu.wodrobina.ragencoderapplication.index;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record VectorDocument(
        String id,
        String content,
        List<Float> embedding,
        String sourceId,
        int chunkIndex,
        String contentHash,
        String documentHash,
        String fileName,
        String fileType,
        Instant indexedAt,
        Map<String, Object> metadata
) {
    public VectorDocument(String id, String content, List<Float> embedding, String sourceId, int chunkIndex,
                          String contentHash, String documentHash, String fileName, String fileType,
                          Instant indexedAt, Map<String, Object> metadata) {
        this.id = id;
        this.content = content;
        this.embedding = embedding;
        this.sourceId = sourceId;
        this.chunkIndex = chunkIndex;
        this.contentHash = contentHash;
        this.documentHash = documentHash;
        this.fileName = fileName;
        this.fileType = fileType;
        this.indexedAt = indexedAt;
        this.metadata = metadata;
    }

    public VectorDocument(String id, String content, List<Float> embedding, Map<String, Object> metadata) {
        this(id, content, embedding, "unknown", 0, "", "", "", "", Instant.now(), metadata);
    }
}