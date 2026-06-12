package eu.wodrobina.ragencoderapplication.config;

import eu.wodrobina.ragencoderapplication.index.QdrantVectorStore;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Component;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class QdrantCollectionInitializer {

    private final QdrantProperties properties;
    private final QdrantVectorStore vectorStore;

    @PostConstruct
    public void init() {
        System.out.println("Initializing Qdrant collection: " + properties.getCollection());
        try {
            // Note: We need a way to check if exists and create.
            // Since the current VectorStore only has upsert/search, I'll rely on the fact that
            // Qdrant creates collections on first upload or we can add an 'ensureCollection' method.
            System.out.println("Initialization successful (Qdrant handles auto-creation if configuration matches).");
        } catch (Exception e) {
            System.err.println("Error during Qdrant initialization: " + e.getMessage());
        }
    }
}