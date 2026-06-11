package eu.wodrobina.ragencoderapplication.encoder;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class OllamaEmbeddingProvider implements EmbeddingProvider {

    private final RestClient restClient;
    private final String model;
    private final int dimension;

    public OllamaEmbeddingProvider(
            @Value("${ollama.base-url}") String baseUrl,
            @Value("${embedding.model:bge-m3}") String model,
            @Value("${embedding.dimension:1024}") int dimension
    ) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
        this.model = model;
        this.dimension = dimension;
    }

    @Override
    public List<Float> embedQuery(String text) {
        return embedDocuments(List.of(text)).getFirst();
    }

    @Override
    public List<List<Float>> embedDocuments(List<String> texts) {
        EmbedRequest request = new EmbedRequest(model, texts);

        EmbedResponse response = restClient.post()
                .uri("/api/embed")
                .body(request)
                .retrieve()
                .body(EmbedResponse.class);

        if (response == null || response.embeddings() == null) {
            throw new IllegalStateException("Ollama returned empty embedding response");
        }

        return response.embeddings();
    }

    @Override
    public String modelName() {
        return model;
    }

    @Override
    public int dimension() {
        return dimension;
    }

    public record EmbedRequest(
            String model,
            List<String> input
    ) {}

    public record EmbedResponse(
            List<List<Float>> embeddings,

            @JsonProperty("total_duration")
            Long totalDuration,

            @JsonProperty("load_duration")
            Long loadDuration,

            @JsonProperty("prompt_eval_count")
            Integer promptEvalCount
    ) {}
}