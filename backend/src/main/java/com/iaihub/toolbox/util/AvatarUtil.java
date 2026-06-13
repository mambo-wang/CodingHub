package com.iaihub.toolbox.util;

import com.iaihub.toolbox.exception.AvatarValidationException;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

public final class AvatarUtil {

    private static final Set<String> ALLOWED_EXT = Set.of(
        "jpg", "jpeg", "png", "webp", "gif"
    );

    private static final Set<String> DANGEROUS_EXT = Set.of(
        "svg", "html", "htm", "xml", "js"
    );

    private static final Set<String> ALLOWED_MIME = Set.of(
        "image/jpeg", "image/png", "image/webp", "image/gif"
    );

    private AvatarUtil() {
    }

    public static String validateAndGetExtension(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AvatarValidationException("请选择头像文件");
        }
        String original = file.getOriginalFilename();
        if (original == null || original.isBlank()) {
            throw new AvatarValidationException("文件名无效");
        }
        String ext = extractExtension(original).toLowerCase();
        if (DANGEROUS_EXT.contains(ext)) {
            throw new AvatarValidationException("出于安全考虑, 不支持 " + ext + " 格式");
        }
        if (!ALLOWED_EXT.contains(ext)) {
            throw new AvatarValidationException("仅支持 jpg / png / webp / gif 格式");
        }
        String mime = file.getContentType();
        if (mime == null || !ALLOWED_MIME.contains(mime.toLowerCase())) {
            throw new AvatarValidationException("文件类型与扩展名不匹配");
        }
        return ext;
    }

    public static void validatePathSafe(String userIdStr) {
        if (userIdStr == null || !userIdStr.matches("^\\d+$")) {
            throw new AvatarValidationException("无效的用户 ID");
        }
    }

    public static String normalizeExt(String ext) {
        if (ext == null) return "jpg";
        String e = ext.toLowerCase();
        return "jpeg".equals(e) ? "jpg" : e;
    }

    private static String extractExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot < 0 ? "" : filename.substring(dot + 1);
    }
}
