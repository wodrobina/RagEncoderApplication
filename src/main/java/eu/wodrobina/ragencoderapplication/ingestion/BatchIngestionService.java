package eu.wodrobina.ragencoderapplication.ingestion;

import eu.wodrobina.ragencoderapplication.index.IndexingService;
import eu.wodrobina.ragencoderapplication.scanner.FileScanner;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BatchIngestionService {

    private final FileScanner fileScanner;
    private final IndexingService indexingService;

    public BatchIngestionService(FileScanner fileScanner, IndexingService indexingService) {
        this.fileScanner = fileScanner;
        this.indexingService = indexingService;
    }

    /**
     * Scans the configured directory and indexes all found files.
     */
    public void runIngestion() throws IOException {
        List<Path> files = fileScanner.scan();
        if (files.isEmpty()) {
            System.out.println("No files found to ingest.");
            return;
        }

        for (Path path : files) {
            String content = fileScanner.readContent(path);
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("file", path.toString());
            metadata.put("type", "document");

            indexingService.indexText(path.toString(), content, metadata);
        }
    }
}
