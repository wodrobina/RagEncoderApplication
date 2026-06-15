package eu.wodrobina.ragencoderapplication.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "rag.reranking")
public class RerankingProperties {

    public static final String STRATEGY_NO_OP = "no-op";
    public static final String STRATEGY_LEXICAL_OVERLAP = "lexical-overlap";

    private boolean enabled = true;

    @NotBlank
    private String strategy = STRATEGY_NO_OP;

    @Min(1)
    @Max(200)
    private int candidateLimit = 20;

    @Min(0)
    @Max(1)
    private double vectorWeight = 0.7;

    @Min(0)
    @Max(1)
    private double lexicalWeight = 0.3;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }

    public int getCandidateLimit() {
        return candidateLimit;
    }

    public void setCandidateLimit(int candidateLimit) {
        this.candidateLimit = candidateLimit;
    }

    public double getVectorWeight() {
        return vectorWeight;
    }

    public void setVectorWeight(double vectorWeight) {
        this.vectorWeight = vectorWeight;
    }

    public double getLexicalWeight() {
        return lexicalWeight;
    }

    public void setLexicalWeight(double lexicalWeight) {
        this.lexicalWeight = lexicalWeight;
    }

    public boolean isNoOpStrategy() {
        return STRATEGY_NO_OP.equalsIgnoreCase(strategy);
    }

    public boolean isLexicalOverlapStrategy() {
        return STRATEGY_LEXICAL_OVERLAP.equalsIgnoreCase(strategy);
    }
}