package eu.wodrobina.ragencoderapplication.config;

import eu.wodrobina.ragencoderapplication.reranking.LexicalOverlapReranker;
import eu.wodrobina.ragencoderapplication.reranking.NoOpReranker;
import eu.wodrobina.ragencoderapplication.reranking.Reranker;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RerankingProperties.class)
public class RerankingConfiguration {

    @Bean
    public Reranker reranker(RerankingProperties properties) {
        if (!properties.isEnabled() || properties.isNoOpStrategy()) {
            return new NoOpReranker();
        }

        if (properties.isLexicalOverlapStrategy()) {
            return new LexicalOverlapReranker(
                    properties.getVectorWeight(),
                    properties.getLexicalWeight()
            );
        }

        throw new IllegalArgumentException(
                "Unsupported reranking strategy: " + properties.getStrategy()
                        + ". Supported values: "
                        + RerankingProperties.STRATEGY_NO_OP + ", "
                        + RerankingProperties.STRATEGY_LEXICAL_OVERLAP
        );
    }
}