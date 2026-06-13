package com.iaihub.toolbox.controller;

import com.iaihub.toolbox.config.UploadConfig;
import com.iaihub.toolbox.util.AvatarUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;

@RestController
@RequestMapping("/api/v1/static/avatars")
@RequiredArgsConstructor
@Slf4j
public class AvatarStaticController {

    private static final List<String> PROBE_ORDER = List.of("jpg", "png", "webp", "gif", "jpeg");

    private final UploadConfig uploadConfig;

    @GetMapping("/{userId}")
    public ResponseEntity<Resource> getAvatar(@PathVariable String userId) {
        // URL 中 userId 可能包含扩展名（如 /avatars/2.jpg），提取纯数字 ID
        String cleanUserId = extractUserId(userId);

        // 防止路径穿越
        AvatarUtil.validatePathSafe(cleanUserId);

        Path avatarDir = Paths.get(uploadConfig.getBaseDir(), uploadConfig.getAvatarSubdir());
        if (!Files.exists(avatarDir)) {
            return ResponseEntity.notFound().build();
        }

        // 如果 URL 已指定扩展名，优先尝试该扩展名
        String urlExt = extractExt(userId);
        if (urlExt != null) {
            Path candidate = avatarDir.resolve(cleanUserId + "." + urlExt);
            if (Files.exists(candidate)) {
                return serveAvatarFile(candidate, urlExt);
            }
        }

        // 否则按 PROBE_ORDER 探测
        for (String ext : PROBE_ORDER) {
            Path candidate = avatarDir.resolve(cleanUserId + "." + ext);
            if (Files.exists(candidate)) {
                return serveAvatarFile(candidate, ext);
            }
        }

        return ResponseEntity.notFound().build();
    }

    /**
     * 从路径变量中提取纯数字 user ID。例如 "2.jpg" → "2", "42" → "42"
     */
    static String extractUserId(String rawUserId) {
        if (rawUserId == null) return "";
        int dot = rawUserId.lastIndexOf('.');
        return dot > 0 ? rawUserId.substring(0, dot) : rawUserId;
    }

    /**
     * 从路径变量中提取扩展名（小写），无扩展名时返回 null
     */
    static String extractExt(String rawUserId) {
        if (rawUserId == null) return null;
        int dot = rawUserId.lastIndexOf('.');
        if (dot > 0 && dot < rawUserId.length() - 1) {
            return rawUserId.substring(dot + 1).toLowerCase();
        }
        return null;
    }

    private ResponseEntity<Resource> serveAvatarFile(Path filePath, String ext) {
        Resource resource = new FileSystemResource(filePath);
        MediaType mediaType = "jpg".equals(ext) || "jpeg".equals(ext)
            ? MediaType.IMAGE_JPEG
            : MediaType.parseMediaType("image/" + ext);
        return ResponseEntity.ok()
            .contentType(mediaType)
            .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePublic())
            .body(resource);
    }
}
