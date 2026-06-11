package eu.wodrobina.ragencoderapplication.chunking;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;

public abstract class AbstractChunker implements Chunker {

    protected static String normalize(String text) {
        return text
                .replace("\r\n", "\n")
                .replace("\r", "\n")
                .trim();
    }

    protected static String stableId(String sourceId, int index, String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(sourceId.getBytes(StandardCharsets.UTF_8));
            digest.update((byte) ':');
            digest.update(Integer.toString(index).getBytes(StandardCharsets.UTF_8));
            digest.update((byte) ':');
            digest.update(content.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(digest.digest());
        } catch (Exception e) {
            throw new IllegalStateException("Could not generate chunk id", e);
        }
    }

    protected Map<String, Object> withChunkMetadata(Map<String, Object> metadata, int chunkIndex, int startOffset, int endOffset) {
        var copy = new java.util.HashMap<>(metadata);
        copy.put("chunk_index", chunkIndex);
        copy.put("start_offset", startOffset);
        copy.put("end_offset", endOffset);
        copy.put("chunker", "smart-line-v1");
        return Map.copyOf(copy);
    }
}
