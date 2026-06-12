package eu.wodrobina.ragencoderapplication.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Configuration
@ConfigurationProperties(prefix = "qdrant")
public class QdrantProperties {
    private String host = "192.168.87.80";
    private Integer grpcPort = 6334;
    private Integer restPort = 6333;
    private String apiKey;
    private String collection = "documents";
    private Integer vectorSize = 1024;
    private String distance = "Cosine";
    private boolean useTls = false;

    // Getters and Setters
    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }
    public Integer getGrpcPort() { return grpcPort; }
    public void setGrpcPort(Integer grpcPort) { this.grpcPort = grpcPort; }
    public Integer getRestPort() { return restPort; }
    public void setRestPort(Integer restPort) { this.restPort = restPort; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getCollection() { return collection; }
    public void setCollection(String collection) { this.collection = collection; }
    public Integer getVectorSize() { return vectorSize; }
    public void setVectorSize(Integer vectorSize) { this.vectorSize = vectorSize; }
    public String getDistance() { return distance; }
    public void setDistance(String distance) { this.distance = distance; }
    public boolean isUseTls() { return useTls; }
    public void setUseTls(boolean useTls) { this.useTls = useTls; }
}