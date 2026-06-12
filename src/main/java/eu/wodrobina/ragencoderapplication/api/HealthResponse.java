package eu.wodrobina.ragencoderapplication.api;

public record HealthResponse(
        String status,
        String qdrant,
        String ollama
) {}