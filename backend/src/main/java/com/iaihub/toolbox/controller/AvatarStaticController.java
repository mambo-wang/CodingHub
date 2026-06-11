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
        // 防止路径穿越
        AvatarUtil.validatePathSafe(userId);

        Path avatarDir = Paths.get(uploadConfig.getBaseDir(), uploadConfig.getAvatarSubdir());
        if (!Files.exists(avatarDir)) {
            return ResponseEntity.notFound().build();
        }

        for (String ext : PROBE_ORDER) {
            Path candidate = avatarDir.resolve(userId + "." + ext);
            if (Files.exists(candidate)) {
                Resource resource = new FileSystemResource(candidate);
                MediaType mediaType = "jpg".equals(ext) || "jpeg".equals(ext)
                    ? MediaType.IMAGE_JPEG
                    : MediaType.parseMediaType("image/" + ext);
                return ResponseEntity.ok()
                    .contentType(mediaType)
                    .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePublic())
                    .body(resource);
            }
        }

        return ResponseEntity.notFound().build();
    }
}
