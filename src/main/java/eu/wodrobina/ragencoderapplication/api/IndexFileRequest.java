package eu.wodrobina.ragencoderapplication.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record IndexFileRequest(
        @NotBlank(message = "Path is required") String path,
        @NotBlank(message = "Source ID is required") String sourceId
) {}