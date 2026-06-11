package eu.wodrobina.ragencoderapplication.index;

import java.util.List;
import java.util.Map;

public record VectorDocument(
        String id,
        String content,
        List<Float> embedding,
        Map<String, Object> metadata
) {}