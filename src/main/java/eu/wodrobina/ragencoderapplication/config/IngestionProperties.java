package eu.wodrobina.ragencoderapplication.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "ingestion")
public class IngestionProperties {
    private String inputDir;
    private List<String> allowedExtensions;

    public String getInputDir() { return inputDir; }
    public void setInputDir(String inputDir) { this.inputDir = inputDir; }
    public List<String> getAllowedExtensions() { return allowedExtensions; }
    public void setAllowedExtensions(List<String> allowedExtensions) { this.allowedExtensions = allowedExtensions; }
}