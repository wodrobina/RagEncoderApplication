package eu.wodrobina.ragencoderapplication.encoder;

import java.util.List;

public interface EmbeddingProvider {
    List<Float> embedQuery(String text);

    List<List<Float>> embedDocuments(List<String> texts);

    String modelName();

    int dimension();
}