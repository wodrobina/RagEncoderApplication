package eu.wodrobina.ragencoderapplication.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Configuration
@ConfigurationProperties(prefix = "ollama")
public class OllamaProperties {
    private String baseUrl;
    private String connectTimeout;
    private String readTimeout;

    // Getters and Setters
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getConnectionTimeout() { return connectTimeout; }
    public void setConnectionTimeout(String connectTimeout) { this.connectTimeout = connectTimeout; }
    public String getReadTimeout() { return readTimeout; }
    public void setReadTimeout(String readTimeout) { this.readTimeout = readTimeout; }
}