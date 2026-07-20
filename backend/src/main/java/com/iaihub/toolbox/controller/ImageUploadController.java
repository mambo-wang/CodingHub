package com.iaihub.toolbox.controller;

import com.iaihub.toolbox.config.UploadConfig;
import com.iaihub.toolbox.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 通用图片上传与访问接口（论坛帖子、评论等场景）。
 * 图片存储在 {baseDir}/images/ 目录下，文件名为 UUID + 原始扩展名。
 */
@RestController
@RequestMapping("/api/v1/uploads/images")
@RequiredArgsConstructor
@Slf4j
public class ImageUploadController {

    private final UploadConfig uploadConfig;

    private static final List<String> ALLOWED_IMAGE_EXTENSIONS = List.of("jpg", "jpeg", "png", "gif", "webp", "svg");
    private static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024; // 10MB

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        // 校验文件
        if (file.isEmpty()) {
            return ApiResponse.error(400, "请选择要上传的图片");
        }
        if (file.getSize() > MAX_IMAGE_SIZE) {
            return ApiResponse.error(400, "图片大小不能超过 10MB");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = getExtension(originalFilename);
        if (!ALLOWED_IMAGE_EXTENSIONS.contains(extension.toLowerCase())) {
            return ApiResponse.error(400, "不支持的图片格式，仅支持: " + String.join(", ", ALLOWED_IMAGE_EXTENSIONS));
        }

        try {
            Path imagesDir = Paths.get(uploadConfig.getBaseDir(), "images");
            if (!Files.exists(imagesDir)) {
                Files.createDirectories(imagesDir);
            }

            String storedName = UUID.randomUUID() + "." + extension.toLowerCase();
            Path targetPath = imagesDir.resolve(storedName);
            file.transferTo(targetPath.toFile());

            String url = "/api/v1/uploads/images/" + storedName;
            log.info("图片上传成功: {} -> {}", originalFilename, storedName);

            return ApiResponse.success("图片上传成功", Map.of("url", url, "filename", storedName));
        } catch (IOException e) {
            log.error("图片上传失败", e);
            return ApiResponse.error(500, "图片上传失败，请重试");
        }
    }

    @GetMapping("/{filename}")
    public ResponseEntity<Resource> getImage(@PathVariable String filename) {
        // 防止路径穿越
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            return ResponseEntity.badRequest().build();
        }

        Path imagePath = Paths.get(uploadConfig.getBaseDir(), "images", filename);
        if (!Files.exists(imagePath)) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(imagePath);
        String contentType = guessContentType(filename);

        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1);
    }

    private String guessContentType(String filename) {
        String ext = getExtension(filename).toLowerCase();
        return switch (ext) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            case "svg" -> "image/svg+xml";
            default -> "application/octet-stream";
        };
    }
}
