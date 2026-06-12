package eu.wodrobina.ragencoderapplication.index;

import eu.wodrobina.ragencoderapplication.config.QdrantProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.http.MediaType;
import java.util.*;

@Component
public class QdrantVectorStore implements VectorStore {

    private final RestClient restClient;
    private final QdrantProperties properties;

    public QdrantVectorStore(RestClient.Builder builder, QdrantProperties properties) {
        this.properties = properties;
        this.restClient = builder
                .baseUrl("http://" + properties.getHost() + ":" + properties.getRestPort())
                .defaultContentType(MediaType.APPLICATION_JSON)
                .build();
    }

    @Override
    public void upsert(List<VectorDocument> documents) {
        if (documents.isEmpty()) return;

        try {
            String endpoint = "/collections/" + properties.getCollection() + "/points?params=wait=1000";
            restClient.post()
                    .uri(endpoint)
                    .body(generatePointsPayload(documents))
                    .retrieve();
            System.out.println("Successfully upserted " + documents.size() + " points to Qdrant.");
        } catch (Exception e) {
            System.err.println("Error uploading to Qdrant: " + e.getMessage());
        }
    }

    @Override
    public List<SearchResult> search(List<Float> queryVector, int limit, Map<String, Object> filter) {
        try {
            String endpoint = "/collections/" + properties.getCollection() + "/points/search";

            Map<String, Object> searchRequest = Map.of(
                    "vector", queryVector,
                    "filter", filter != null ? filter : Map.of(),
                    "limit", limit
            );

            SearchResultResponse response = restClient.post()
                    .uri(endpoint)
                    .body(searchRequest)
                    .retrieve()
                    .body(SearchResultResponse.class);

            return response.points();
        } catch (Exception e) {
            System.err.println("Error searching Qdrant: " + e.getMessage());
            return List.of();
        }
    }

    private Map<String, Object> generatePointsPayload(List<VectorDocument> documents) {
        List<Map<String, Object>> points = new ArrayList<>();
        for (VectorDocument doc : documents) {
            Map<String, Object> point = new HashMap<>();
            point.put("id", doc.id());
            point.put("vector", doc.embedding());

            Map<String, Object> payload = new HashMap<>();
            payload.put("content", doc.content());
            payload.put("sourceId", doc.sourceId());
            payload.put("chunkIndex", doc.chunkIndex());
            payload.put("fileName", doc.fileName());
            payload.put("fileType", doc.fileType());
            payload.put("indexedAt", doc.indexedAt().toString());

            point.put("payload", payload);
            points.add(point);
        }
        return Map.of("points", points);
    }

    private static class SearchResultResponse {
        private List<SearchResult> points;
        public List<SearchResult> getPoints() { return points; }
        public void setPoints(List<SearchResult> points) { this.points = points; }
    }
}