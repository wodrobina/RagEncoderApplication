package eu.wodrobina.ragencoderapplication.scanner;

import eu.wodrobina.ragencoderapplication.config.FileScannerProperties;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class FileScanner {

    private final FileScannerProperties properties;

    public FileScanner(FileScannerProperties properties) {
        this.properties = properties;
    }

    /**
     * Scans the root path for files matching specified extensions recursively.
     * @return a list of Paths to found files.
     */
    public List<Path> scan() throws IOException {
        if (properties.getRootPath() == null) {
            return List.of();
        }

        try (Stream<Path> paths = Files.walk(Paths.get(properties.getRootPath()))) {
            return paths.filter(Files::isRegularFile)
                    .filter(this::isIncluded)
                    .collect(Collectors.toList());
        }
    }

    /**
     * Reads the content of a file.
     * @param path The path to the file.
     * @return The content as a String.
     */
    public String readContent(Path path) throws IOException {
        return Files.readString(path);
    }

    private boolean isIncluded(Path path) {
        if (properties.getIncludeExtensions() == null || properties.getIncludeExtensions().isEmpty()) {
            return true;
        }

        String fileName = path.toString().toLowerCase();
        return properties.getIncludeExtensions().stream()
                .anyMatch(ext -> fileName.endsWith(ext.toLowerCase()));
    }
}
