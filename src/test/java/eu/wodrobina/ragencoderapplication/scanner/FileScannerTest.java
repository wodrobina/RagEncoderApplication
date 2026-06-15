package eu.wodrobina.ragencoderapplication.scanner;

import eu.wodrobina.ragencoderapplication.config.FileScannerProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.context.ContextConfiguration;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FileScannerTest {

    @TempDir
    Path tempDir;

    private FileScanner fileScanner;
    private FileScannerProperties properties;

    @BeforeEach
    void setUp() {
        properties = new FileScannerProperties();
        // Default setup for tests
        properties.setRootPath(tempDir.toString());
        properties.setIncludeExtensions(List.of(".txt", ".md"));
        fileScanner = new FileScanner(properties);
    }

    @Test
    void testFileScanner_shouldFindAllSupportedFiles() throws IOException {
        // Create sample files
        Files.createFile(tempDir.resolve("test1.txt"));
        Files.createFile(tempDir.resolve("test2.md"));
        Files.createFile(tempDir.resolve("ignored.pdf"));

        List<Path> results = fileScanner.scan();

        assertEquals(2, results.size(), "Should find exactly 2 files with supported extensions");
        assertTrue(results.stream().anyMatch(p -> p.toString().endsWith("test1.txt")));
        assertTrue(results.stream().anyMatch(p -> p.toString().endsWith("test2.md")));
    }

    @Test
    void testFileScanner_shouldFindNoFilesWhenRootDirIsEmpty() throws IOException {
        List<Path> results = fileScanner.scan();
        assertTrue(results.isEmpty());
    }

    @Test
    void testFileScanner_shouldIncludeAllTextFilesWhenNoExtensionsAreSpecified() throws IOException {
        properties.setIncludeExtensions(null);
        Files.createFile(tempDir.resolve("test1.txt"));
        Files.createFile(tempDir.resolve("ignored.pdf"));

        List<Path> results = fileScanner.scan();

        assertEquals(1, results.size());
    }
}
