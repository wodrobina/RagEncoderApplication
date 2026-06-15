package eu.wodrobina.ragencoderapplication.api;

import jakarta.validation.constraints.*;
import java.util.Map;

public record SearchRequest(
    @NotBlank(message = "Query is required") String query,
    @Min(1) @Max(50) int limit,
    Map<String, Object> filter,
    String collection,
    Boolean rerank,
    Integer candidateLimit
) {}
