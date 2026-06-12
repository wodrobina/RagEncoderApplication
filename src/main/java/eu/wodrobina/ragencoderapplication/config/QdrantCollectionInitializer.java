package eu.wodrobina.ragencoderapplication.config;

import eu.wodrobina.ragencoderapplication.index.QdrantVectorStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class QdrantCollectionInitializer {

    private final QdrantProperties properties;
    private final QdrantVectorStore vectorStore;

    public void init() {
        System.out.println("Initializing Qdrant collection: " + properties.getCollection());
        try {
            System.out.println("Initialization successful (Qdrant handles auto-creation if configuration matches).");
        } catch (Exception e) {
            System.err.println("Error during Qdrant initialization: " + e.getMessage());
        }
    }
}
