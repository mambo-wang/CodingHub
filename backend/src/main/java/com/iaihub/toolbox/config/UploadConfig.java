package com.iaihub.toolbox.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "app.upload")
@Data
public class UploadConfig {

    private String baseDir = "uploads/tools";
    private String maxFileSize = "50MB";
    private String maxRequestSize = "200MB";
    private List<String> allowedExtensions;
}
