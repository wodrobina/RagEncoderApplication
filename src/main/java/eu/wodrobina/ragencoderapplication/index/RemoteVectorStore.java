package eu.wodrobina.ragencoderapplication.index;

import eu.wodrobina.ragencoderapplication.config.VectorStoreProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
public class RemoteVectorStore implements VectorStore {

    private final RestClient restClient;

    public RemoteVectorStore(VectorStoreProperties properties) {
        this.restClient =RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .build();
    }

    @Override
    public void upsert(List<VectorDocument> documents) {
        // Implementation for posting to the remote vector store API
        // This is a placeholder for now as we don't have an actual external service
        System.out.println("Upserting " + documents.size() + " documents to: " +
            "http://" + System.getenv("VECTOR_STORE_URL") + "/upsert");
    }

    @Override
    public List<SearchResult> search(List<Float> queryVector, int limit, Map<String, Object> filter) {
        // Placeholder for search implementation
        return List.of();
    }
}
