package eu.wodrobina.ragencoderapplication.index;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RagConfig {

    @Bean
    public VectorStore vectorStore() {
        return new InMemoryVectorStore();
    }
}