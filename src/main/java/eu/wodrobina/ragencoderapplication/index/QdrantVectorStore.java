package eu.wodrobina.ragencoderapplication.index;

import eu.wodrobina.ragencoderapplication.config.QdrantProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class QdrantVectorStore implements VectorStore {

    private static final Logger log = LoggerFactory.getLogger(QdrantVectorStore.class);

    private final RestClient restClient;
    private final QdrantProperties properties;

    public QdrantVectorStore(RestClient.Builder builder, QdrantProperties properties) {
        this.properties = properties;
        this.restClient = builder
                .baseUrl("http://" + properties.getHost() + ":" + properties.getRestPort())
                .build();
    }

    @Override
    public void upsert(List<VectorDocument> documents, String collection) {
        if (documents == null || documents.isEmpty()) {
            return;
        }

        ensureCollectionExists(collection);

        try {
            restClient.put()
                    .uri("/collections/{collection}/points?wait=true", collection)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(generatePointsPayload(documents))
                    .retrieve()
                    .toBodilessEntity();

            log.info("Successfully upserted {} points to collection: {}", documents.size(), collection);
        } catch (Exception e) {
            log.error("Error uploading to Qdrant for collection {}: {}", collection, e.getMessage(), e);
            throw new RuntimeException("Failed to upload data to Qdrant", e);
        }
    }

    @Override
    public List<SearchResult> search(
            List<Float> queryVector,
            int limit,
            Map<String, Object> filter,
            String collection
    ) {
        if (queryVector == null || queryVector.isEmpty()) {
            return List.of();
        }

        ensureCollectionExists(collection);

        try {
            Map<String, Object> searchRequest = new HashMap<>();
            searchRequest.put("vector", queryVector);
            searchRequest.put("limit", limit);
            searchRequest.put("with_payload", true);

            if (filter != null && !filter.isEmpty()) {
                searchRequest.put("filter", toQdrantFilter(filter));
            }

            QdrantSearchResponse response = restClient.post()
                    .uri("/collections/{collection}/points/search", collection)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(searchRequest)
                    .retrieve()
                    .body(QdrantSearchResponse.class);

            if (response == null || response.getResult() == null) {
                return List.of();
            }

            return response.getResult()
                    .stream()
                    .map(QdrantPointResult::toSearchResult)
                    .toList();

        } catch (Exception e) {
            log.error("Error searching Qdrant in collection {}: {}", collection, e.getMessage(), e);
            return List.of();
        }
    }

    private Map<String, Object> toQdrantFilter(Map<String, Object> filter) {
        if (filter.containsKey("must")
                || filter.containsKey("should")
                || filter.containsKey("must_not")
                || filter.containsKey("min_should")) {
            return filter;
        }

        List<Map<String, Object>> must = filter.entrySet()
                .stream()
                .filter(entry -> entry.getValue() != null)
                .map(entry -> Map.of(
                        "key", entry.getKey(),
                        "match", Map.of("value", entry.getValue())
                ))
                .toList();

        return Map.of("must", must);
    }

    private void ensureCollectionExists(String collectionName) {
        try {
            Map<String, Object> payload = Map.of(
                    "vectors", Map.of(
                            "size", properties.getVectorSize(),
                            "distance", properties.getDistance()
                    )
            );

            restClient.put()
                    .uri("/collections/{collection}", collectionName)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();

            log.info(
                    "Created Qdrant collection: {}, vectorSize={}, distance={}",
                    collectionName,
                    properties.getVectorSize(),
                    properties.getDistance()
            );

        } catch (HttpClientErrorException.Conflict e) {
            log.debug("Qdrant collection already exists: {}", collectionName);
        } catch (Exception e) {
            log.error("Error creating Qdrant collection {}: {}", collectionName, e.getMessage(), e);
            throw new RuntimeException("Failed to create Qdrant collection: " + collectionName, e);
        }
    }

    private Map<String, Object> generatePointsPayload(List<VectorDocument> documents) {
        List<Map<String, Object>> points = new ArrayList<>();

        for (VectorDocument doc : documents) {
            Map<String, Object> point = new HashMap<>();

            String qdrantId = UUID.nameUUIDFromBytes(
                    doc.id().getBytes(StandardCharsets.UTF_8)
            ).toString();

            point.put("id", qdrantId);
            point.put("vector", doc.embedding());

            Map<String, Object> payload = new HashMap<>();

            if (doc.metadata() != null) {
                payload.putAll(doc.metadata());
            }

            payload.put("originalId", doc.id());
            payload.put("content", doc.content());
            payload.put("sourceId", doc.sourceId());
            payload.put("chunkIndex", doc.chunkIndex());
            payload.put("contentHash", doc.contentHash());
            payload.put("documentHash", doc.documentHash());
            payload.put("fileName", doc.fileName());
            payload.put("fileType", doc.fileType());

            if (doc.indexedAt() != null) {
                payload.put("indexedAt", doc.indexedAt().toString());
            }

            point.put("payload", payload);
            points.add(point);
        }

        return Map.of("points", points);
    }

    private static class QdrantSearchResponse {
        private List<QdrantPointResult> result;

        public List<QdrantPointResult> getResult() {
            return result;
        }

        public void setResult(List<QdrantPointResult> result) {
            this.result = result;
        }
    }

    private static class QdrantPointResult {
        private String id;
        private Float score;
        private Map<String, Object> payload;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public Float getScore() {
            return score;
        }

        public void setScore(Float score) {
            this.score = score;
        }

        public Map<String, Object> getPayload() {
            return payload;
        }

        public void setPayload(Map<String, Object> payload) {
            this.payload = payload;
        }

        public SearchResult toSearchResult() {
            String content = null;

            if (payload != null && payload.get("content") != null) {
                content = payload.get("content").toString();
            }

            return new SearchResult(
                    id,
                    content,
                    score,
                    payload
            );
        }
    }
}