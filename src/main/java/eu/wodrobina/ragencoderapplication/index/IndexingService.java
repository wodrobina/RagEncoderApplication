package eu.wodrobina.ragencoderapplication.index;

import eu.wodrobina.ragencoderapplication.chunking.Chunk;
import eu.wodrobina.ragencoderapplication.chunking.TextChunker;
import eu.wodrobina.ragencoderapplication.encoder.EmbeddingProvider;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class IndexingService {

    private final TextChunker chunker;
    private final EmbeddingProvider embeddingProvider;
    private final VectorStore vectorStore;

    public IndexingService(
            TextChunker chunker,
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
            throw new IllegalStateException("Embedding count does not match chunk count");
        }

        List<VectorDocument> documents = new ArrayList<>();

        for (int i = 0; i < chunks.size(); i++) {
            Chunk chunk = chunks.get(i);

            documents.add(new VectorDocument(
                    chunk.id(),
                    chunk.content(),
                    embeddings.get(i),
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