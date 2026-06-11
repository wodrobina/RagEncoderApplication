package eu.wodrobina.ragencoderapplication.chunking;

import java.util.List;
import java.util.Map;

public interface Chunker {
    List<Chunk> chunk(String sourceId, String text, Map<String, Object> metadata);
}