package eu.wodrobina.ragencoderapplication.chunking;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Primary;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

@Component
@Primary
public class SmartChunker extends AbstractChunker {

    private final int chunkSize;
    private final int overlap;

    public SmartChunker(
            @Value("${rag.chunk-size:1200}") int chunkSize,
            @Value("${rag.chunk-overlap:200}") int overlap
    ) {
        this.chunkSize = chunkSize;
        this.overlap = overlap;
    }

    @Override
    public List<Chunk> chunk(String sourceId, String text, Map<String, Object> metadata) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        String normalized = normalize(text);
        String[] lines = normalized.split("\\R");
        List<Chunk> chunks = new ArrayList<>();

        int currentLineIdx = 0;
        int chunkCount = 0;

        while (currentLineIdx < lines.length) {
            // Case 1: Single line is longer than chunkSize -> split it into character-based chunks
            if (lines[currentLineIdx].length() > chunkSize) {
                String content = lines[currentLineIdx];
                int startChar = 0;
                int internalCount = 0;

                while (startChar < content.length()) {
                    int endChar = Math.min(startChar + chunkSize, content.length());
                    String subContent = content.substring(startChar, endChar);

                    int currentStartOffset = calculateOffset(lines, currentLineIdx) + startChar;
                    int currentEndOffset = currentStartOffset + subContent.length();

                    chunks.add(new Chunk(
                            stableId(sourceId, chunkCount++, subContent),
                            subContent,
                            withChunkMetadata(metadata, chunkCount - 1, currentStartOffset, currentEndOffset),
                            sourceId,
                            chunkCount - 1,
                            generateHash(subContent),
                            generateHash(subContent),
                            "unknown",
                            "unknown"
                    ));

                    startChar = endChar;
                    internalCount++;
                }
                currentLineIdx++;
                continue;
            }

            // Case 2: Standard line-based chunking
            int endOfChunkIdx = currentLineIdx;
            int currentSize = 0;

            for (int i = currentLineIdx; i < lines.length; i++) {
                int lineLen = lines[i].length() + 1;
                if (currentSize + lineLen > chunkSize && i > currentLineIdx) {
                    break;
                }
                currentSize += lineLen;
                endOfChunkIdx = i;
            }

            StringBuilder sb = new StringBuilder();
            int startOffset = calculateOffset(lines, currentLineIdx);
            for (int i = currentLineIdx; i <= endOfChunkIdx; i++) {
                sb.append(lines[i]);
                if (i < endOfChunkIdx) {
                    sb.append(System.lineSeparator());
                }
            }

            String content = sb.toString();
            int endOffset = startOffset + content.length();

            chunks.add(new Chunk(
                    stableId(sourceId, chunkCount++, content),
                    content,
                    withChunkMetadata(metadata, chunkCount - 1, startOffset, endOffset),
                    sourceId,
                    chunkCount - 1,
                    generateHash(content),
                    generateHash(content),
                    "unknown",
                    "unknown"
            ));

            // Determine next starting index (overlap)
            int nextStartIdx = calculateNextStartIndex(lines, endOfChunkIdx);

            if (nextStartIdx <= currentLineIdx) {
                currentLineIdx = endOfChunkIdx + 1;
            } else {
                currentLineIdx = nextStartIdx;
            }
        }

        return chunks;
    }

    private int calculateNextStartIndex(String[] lines, int currentEndIdx) {
        int overlapLinesCount = 0;
        int runningLen = 0;
        for (int i = currentEndIdx; i > 0; i--) {
            int lLen = lines[i].length() + 1;
            if (runningLen + lLen > overlap) break;
            runningLen += lLen;
            overlapLinesCount++;
        }
        return Math.max(0, currentEndIdx - overlapLinesCount + 1);
    }

    private int calculateOffset(String[] lines, int lineIdx) {
        int offset = 0;
        for (int i = 0; i < lineIdx; i++) {
            offset += lines[i].length() + 1;
        }
        return offset;
    }

    private static String generateHash(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            return "";
        }
    }
}
