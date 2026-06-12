package eu.wodrobina.ragencoderapplication.scanner;

import eu.wodrobina.ragencoderapplication.config.FileScannerProperties;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class FileScanner {

    private final FileScannerProperties properties;

    public FileScanner(FileScannerProperties properties) {
        this.properties = properties;
    }

    public List<Path> scan() throws IOException {
        if (properties.getRootPath() == null || properties.getRootPath().isBlank()) {
            return List.of();
        }

        return scan(Path.of(properties.getRootPath()));
    }

    public List<Path> scan(Path rootPath) throws IOException {
        if (rootPath == null) {
            return List.of();
        }

        Path resolvedRootPath = resolvePath(rootPath);

        if (!Files.exists(resolvedRootPath)) {
            throw new IllegalArgumentException("Path does not exist: " + resolvedRootPath);
        }

        if (!Files.isDirectory(resolvedRootPath)) {
            throw new IllegalArgumentException("Path is not a directory: " + resolvedRootPath);
        }

        try (Stream<Path> paths = Files.walk(resolvedRootPath)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(this::isIncluded)
                    .collect(Collectors.toList());
        }
    }

    public String readContent(Path path) throws IOException {
        String fileName = path.getFileName().toString().toLowerCase();

        if (!isTextFile(fileName)) {
            throw new IllegalArgumentException("Unsupported text file type: " + path);
        }

        return Files.readString(path);
    }

    private Path resolvePath(Path path) {
        String rawPath = path.toString();

        if (rawPath.equals("~")) {
            return Path.of(System.getProperty("user.home")).toAbsolutePath().normalize();
        }

        if (rawPath.startsWith("~/")) {
            return Path.of(System.getProperty("user.home"), rawPath.substring(2))
                    .toAbsolutePath()
                    .normalize();
        }

        return path.toAbsolutePath().normalize();
    }

    private boolean isIncluded(Path path) {
        String fileName = path.getFileName().toString().toLowerCase();

        if (properties.getIncludeExtensions() == null || properties.getIncludeExtensions().isEmpty()) {
            return isTextFile(fileName);
        }

        String fullPath = path.toString().toLowerCase();
        return properties.getIncludeExtensions().stream()
                .anyMatch(ext -> fullPath.endsWith(normalizeExtension(ext)));
    }

    private boolean isTextFile(String fileName) {
        return fileName.endsWith(".txt")
                || fileName.endsWith(".md")
                || fileName.endsWith(".java")
                || fileName.endsWith(".kt")
                || fileName.endsWith(".xml")
                || fileName.endsWith(".yml")
                || fileName.endsWith(".yaml")
                || fileName.endsWith(".json")
                || fileName.endsWith(".properties")
                || fileName.endsWith(".gradle")
                || fileName.endsWith(".kts");
    }

    private String normalizeExtension(String extension) {
        String normalized = extension.toLowerCase();

        if (!normalized.startsWith(".")) {
            return "." + normalized;
        }

        return normalized;
    }
}