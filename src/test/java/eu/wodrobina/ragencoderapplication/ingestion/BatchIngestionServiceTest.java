package eu.wodrobina.ragencoderapplication.ingestion;

import eu.wodrobina.ragencoderapplication.index.IndexingService;
import eu.wodrobina.ragencoderapplication.scanner.FileScanner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BatchIngestionServiceTest {

    @Mock
    private FileScanner fileScanner;

    @Mock
    private IndexingService indexingService;

    private BatchIngestionService batchIngestionService;

    @BeforeEach
    void setUp() {
        batchIngestionService = new BatchIngestionService(fileScanner, indexingService);
    }

    @Test
    void BatchIngestion_shouldProcessAllFilesFromScanner() throws IOException {
        // Arrange
        Path file1 = Paths.get("test1.txt");
        Path file2 = Paths.get("test2.md");

        when(fileScanner.scan()).thenReturn(List.of(file1, file2));
        when(fileScanner.readContent(file1)).thenReturn("Content of test 1");
        when(fileScanner.readContent(file2)).thenReturn("Content of test 2");

        // Act
        batchIngestionService.runIngestion();

        // Assert
        verify(indexingService, times(2)).indexText(anyString(), anyString(), anyMap(), anyString());
        verify(indexingService).indexText(eq(file1.toString()), eq("Content of test 1"), anyMap(), eq("documents"));
        verify(indexingService).indexText(eq(file2.toString()), eq("Content of test 2"), anyMap(), eq("documents"));
    }
}
