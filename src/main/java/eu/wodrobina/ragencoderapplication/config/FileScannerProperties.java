package eu.wodrobina.ragencoderapplication.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "file-scanner")
public class FileScannerProperties {
    private String rootPath;
    private List<String> includeExtensions;

    public String getRootPath() {
        return rootPath;
    }

    public void setRootPath(String rootPath) {
        this.rootPath = rootPath;
    }

    public List<String> getIncludeExtensions() {
        return includeExtensions;
    }

    public void setIncludeExtensions(List<String> includeExtensions) {
        this.includeExtensions = includeExtensions;
    }
}
