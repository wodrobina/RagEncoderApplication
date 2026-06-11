package eu.wodrobina.ragencoderapplication.index;

import java.util.List;
import java.util.Map;

public interface VectorStore {
    void upsert(List<VectorDocument> documents);

    List<SearchResult> search(List<Float> queryVector, int limit, Map<String, Object> filter);
}