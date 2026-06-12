package eu.wodrobina.ragencoderapplication.chunking;

import java.util.Map;

public record Chunk(
        String id,
        String content,
        Map<String, Object> metadata,
        String sourceId,
        int chunkIndex,
        String contentHash,
        String documentHash,
        String fileName,
        String fileType
) {}
