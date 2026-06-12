package eu.wodrobina.ragencoderapplication.api;

import jakarta.validation.constraints.NotBlank;

public record IndexDirectoryRequest(
        @NotBlank(message = "Path is required") String path
) {}