package com.iaihub.toolbox.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
@Data
@Slf4j
public class VideoStorageConfig {

    @Value("${app.upload.base-dir:}")
    private String uploadBaseDir;

    private String videoStoragePath;

    @PostConstruct
    public void init() {
        if (uploadBaseDir == null || uploadBaseDir.isBlank()) {
            String userHome = System.getProperty("user.home");
            uploadBaseDir = Paths.get(userHome, "aifiles").toString();
            log.info("未配置上传根目录，使用默认路径: {}", uploadBaseDir);
        }

        Path videoPath = Paths.get(uploadBaseDir, "uploads", "videos");
        videoStoragePath = videoPath.toAbsolutePath().toString();

        if (!Files.exists(videoPath)) {
            try {
                Files.createDirectories(videoPath);
                log.info("创建视频存储目录: {}", videoPath.toAbsolutePath());
            } catch (IOException e) {
                log.error("无法创建视频存储目录: {}", videoPath, e);
                throw new RuntimeException("无法创建视频存储目录: " + videoPath, e);
            }
        }
    }
}
