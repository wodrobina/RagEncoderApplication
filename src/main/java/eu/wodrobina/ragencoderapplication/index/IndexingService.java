package eu.wodrobina.ragencoderapplication.index;

import eu.wodrobina.ragencoderapplication.chunking.Chunk;
import eu.wodrobina.ragencoderapplication.chunking.Chunker;
import eu.wodrobina.ragencoderapplication.encoder.EmbeddingProvider;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.time.Instant;

@Service
public class IndexingService {

    private final Chunker chunker;
    private final EmbeddingProvider embeddingProvider;
    private final VectorStore vectorStore;

    public IndexingService(
            Chunker chunker,
            EmbeddingProvider embeddingProvider,
            VectorStore vectorStore
    ) {
        this.chunker = chunker;
        this.embeddingProvider = embeddingProvider;
        this.vectorStore = vectorStore;
    }

    public int indexText(String sourceId, String text, Map<String, Object> metadata) {
        List<Chunk> chunks = chunker.chunk(sourceId, text, enrichedMetadata(metadata));

        List<String> contents = chunks.stream()
                .map(Chunk::content)
                .toList();

        List<List<Float>> embeddings = embeddingProvider.embedDocuments(contents);

        if (embeddings.size() != chunks.size()) {
            throw new IllegalStateException("Embedding count does not match chunk count: expected " + chunks.size() + ", got " + embeddings.size());
        }

        List<VectorDocument> documents = new ArrayList<>();

        for (int i = 0; i < chunks.size(); i++) {
            Chunk chunk = chunks.get(i);

            documents.add(new VectorDocument(
                    chunk.id(),
                    chunk.content(),
                    embeddings.get(i),
                    chunk.sourceId(),
                    chunk.chunkIndex(),
                    chunk.contentHash(),
                    chunk.documentHash(),
                    chunk.fileName(),
                    chunk.fileType(),
                    Instant.now(),
                    chunk.metadata()
            ));
        }

        vectorStore.upsert(documents);
        return documents.size();
    }

    private Map<String, Object> enrichedMetadata(Map<String, Object> metadata) {
        var copy = new java.util.HashMap<>(metadata);
        copy.put("embedding_model", embeddingProvider.modelName());
        copy.put("embedding_dimension", embeddingProvider.dimension());
        return Map.copyOf(copy);
    }
}
