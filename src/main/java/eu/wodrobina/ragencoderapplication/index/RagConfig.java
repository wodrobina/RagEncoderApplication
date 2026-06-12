package eu.wodrobina.ragencoderapplication.index;

import eu.wodrobina.ragencoderapplication.config.QdrantProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RagConfig {

    @Bean
    public VectorStore vectorStore(RestClient.Builder builder, QdrantProperties properties) {
        return new QdrantVectorStore(builder, properties);
    }

}