package eu.wodrobina.ragencoderapplication.chunking;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

@Component
public class TextChunker {

    private final int chunkSize;
    private final int overlap;

    public TextChunker(
            @Value("${rag.chunk-size:1200}") int chunkSize,
            @Value("${rag.chunk-overlap:200}") int overlap
    ) {
        if (overlap >= chunkSize) {
            throw new IllegalArgumentException("Overlap must be smaller than chunk size");
        }

        this.chunkSize = chunkSize;
        this.overlap = overlap;
    }

    public List<Chunk> chunk(String sourceId, String text, Map<String, Object> metadata) {
        List<Chunk> chunks = new ArrayList<>();

        if (text == null || text.isBlank()) {
            return chunks;
        }

        String normalized = normalize(text);
        int start = 0;
        int index = 0;

        while (start < normalized.length()) {
            int end = Math.min(start + chunkSize, normalized.length());
            String content = normalized.substring(start, end).trim();

            if (!content.isBlank()) {
                chunks.add(new Chunk(
                        stableId(sourceId, index, content),
                        content,
                        withChunkMetadata(metadata, index, start, end)
                ));
            }

            if (end == normalized.length()) {
                break;
            }

            start = end - overlap;
            index++;
        }

        return chunks;
    }

    private static String normalize(String text) {
        return text
                .replace("\r\n", "\n")
                .replace("\r", "\n")
                .trim();
    }

    private static Map<String, Object> withChunkMetadata(
            Map<String, Object> metadata,
            int chunkIndex,
            int startOffset,
            int endOffset
    ) {
        var copy = new HashMap<>(metadata);
        copy.put("chunk_index", chunkIndex);
        copy.put("start_offset", startOffset);
        copy.put("end_offset", endOffset);
        copy.put("chunker", "text-fixed-v1");
        return Map.copyOf(copy);
    }

    private static String stableId(String sourceId, int index, String content) {
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
}