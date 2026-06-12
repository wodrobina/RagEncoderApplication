package eu.wodrobina.ragencoderapplication.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Configuration
@ConfigurationProperties(prefix = "chunking")
public class ChunkingProperties {
    private Integer maxChars;
    private Integer overlapChars;

    // Getters and Setters
    public Integer getMaxChars() { return maxChars; }
    public void setMaxChars(Integer maxChars) { this.maxChars = maxChars; }
    public Integer getOverlapChars() { return overlapChars; }
    public void setOverlapChars(Integer overlapChars) { this.overlapChars = overlapChars; }
}