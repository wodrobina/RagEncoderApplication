package eu.wodrobina.ragencoderapplication.chunking;

import java.util.Map;

public record Chunk(
        String id,
        String content,
        Map<String, Object> metadata
) {}