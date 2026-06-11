package com.iaihub.toolbox.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "app.upload")
@Data
@Slf4j
public class UploadConfig {

    private String baseDir;

    private String maxFileSize = "50MB";

    private String maxRequestSize = "200MB";

    private List<String> allowedExtensions;

    // 头像专属配置
    private String avatarSubdir = "avatars";

    private String avatarMaxFileSize = "2MB";

    private List<String> avatarAllowedExtensions = List.of("jpg", "jpeg", "png", "webp", "gif");

    @PostConstruct
    public void init() {
        if (baseDir == null || baseDir.isBlank()) {
            String userHome = System.getProperty("user.home");
            baseDir = Paths.get(userHome, "aifiles").toString();
            log.info("未配置上传目录，使用默认路径: {}", baseDir);
        }

        Path basePath = Paths.get(baseDir);
        if (!Files.exists(basePath)) {
            try {
                Files.createDirectories(basePath);
                log.info("创建上传目录: {}", basePath.toAbsolutePath());
            } catch (IOException e) {
                log.error("无法创建上传目录: {}", basePath, e);
                throw new RuntimeException("无法创建上传目录: " + baseDir, e);
            }
        }

        // 确保头像子目录存在
        if (avatarSubdir != null && !avatarSubdir.isBlank()) {
            Path avatarPath = Paths.get(baseDir, avatarSubdir);
            if (!Files.exists(avatarPath)) {
                try {
                    Files.createDirectories(avatarPath);
                    log.info("创建头像目录: {}", avatarPath.toAbsolutePath());
                } catch (IOException e) {
                    log.error("无法创建头像目录: {}", avatarPath, e);
                    throw new RuntimeException("无法创建头像目录: " + avatarPath, e);
                }
            }
        }
    }
}
