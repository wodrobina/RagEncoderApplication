package eu.wodrobina.ragencoderapplication.index;

import java.util.List;
import java.util.Map;

public record SearchResult(
        String id,
        String content,
        double score,
        Map<String, Object> metadata
) {}