package eu.wodrobina.ragencoderapplication.chunking;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TextChunkerTest {

    private TextChunker chunker;
    private final int chunkSize = 100;
    private final int overlap = 20;

    @BeforeEach
    void setUp() {
        chunker = new TextChunker(chunkSize, overlap);
    }

    @Test
    void testChunkingLogic() {
        String text = "This is a long string that should be split into several chunks based on the chunk size we provided to the constructor.";
        // length is roughly 115 chars. With 100 size and 20 overlap:
        // Chunk 1: [0, 100]
        // Chunk 2: [80, end]

        List<Chunk> chunks = chunker.chunk("test-id", text, Map.of());

        assertFalse(chunks.isEmpty());
        assertTrue(chunks.size() >= 2);
        assertEquals(text.substring(0, 100).trim(), chunks.get(0).content());
    }

    @Test
    void testOverlapAndMetadata() {
        String text = "The quick brown fox jumps over the lazy dog and then runs away into the forest to find some nuts."; // ~95 chars
        // Let's use a shorter chunk size to force overlap
        TextChunker smallChunker = new TextChunker(30, 10);

        List<Chunk> chunks = smallChunker.chunk("test-id", text, Map.of("key", "value"));

        assertTrue(chunks.size() > 1);

        // Check metadata enrichment
        Map<String, Object> metadata = chunks.get(0).metadata();
        assertEquals("value", metadata.get("key"));
        assertEquals("text-fixed-v1", metadata.get("chunker"));
        assertTrue(metadata.containsKey("chunk_index"));
        assertTrue(metadata.containsKey("start_offset"));
        assertTrue(metadata.containsKey("end_offset"));
    }

    @Test
    void testInvalidConfiguration() {
        assertThrows(IllegalArgumentException.class, () -> new TextChunker(100, 150));
    }

    @Test
    void testEmptyText() {
        List<Chunk> chunks = chunker.chunk("test-id", "   ", Map.of());
        assertTrue(chunks.isEmpty());
    }
}