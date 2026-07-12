package com.iaihub.toolbox.controller;

import com.iaihub.toolbox.dto.*;
import com.iaihub.toolbox.model.Role;
import com.iaihub.toolbox.model.User;
import com.iaihub.toolbox.service.UnifiedCommentService;
import com.iaihub.toolbox.service.UnifiedFavoriteService;
import com.iaihub.toolbox.service.UnifiedLikeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@RestController
@RequestMapping("/api/v1/interactions")
@RequiredArgsConstructor
public class UnifiedInteractionController {

    private final UnifiedLikeService likeService;
    private final UnifiedCommentService commentService;
    private final UnifiedFavoriteService favoriteService;

    // ==================== LIKES ====================

    @PostMapping("/likes")
    public ResponseEntity<ApiResponse<InteractionResponse>> toggleLike(
            @Valid @RequestBody InteractionRequest request,
            HttpServletRequest httpRequest) {

        User user = getCurrentUser();
        Long userId = user != null ? user.getId() : null;
        String ipHash = userId != null ? null : computeIpHash(httpRequest);

        InteractionResponse response = likeService.toggleLike(
                request.getTargetType(), request.getTargetId(), userId, ipHash);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/likes/status")
    public ResponseEntity<ApiResponse<InteractionResponse>> getLikeStatus(
            @RequestParam String targetType,
            @RequestParam Long targetId,
            HttpServletRequest httpRequest) {

        User user = getCurrentUser();
        Long userId = user != null ? user.getId() : null;
        String ipHash = userId != null ? null : computeIpHash(httpRequest);

        InteractionResponse response = likeService.getLikeStatus(targetType, targetId, userId, ipHash);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/likes/mine")
    public ResponseEntity<ApiResponse<PageResponse<?>>> getMyLikes(
            @RequestParam String targetType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).body(ApiResponse.error(401, "点赞查询需要登录"));
        }

        PageResponse<?> response = likeService.getMyLikes(targetType, user.getId(), page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ==================== COMMENTS ====================

    @PostMapping("/comments")
    public ResponseEntity<ApiResponse<InteractionResponse>> addComment(
            @Valid @RequestBody InteractionRequest request,
            HttpServletRequest httpRequest) {

        User user = getCurrentUser();
        Long userId = user != null ? user.getId() : null;
        String userName = request.getUserName();

        InteractionResponse response = commentService.addComment(
                request.getTargetType(), request.getTargetId(),
                userId, userName,
                request.getContent(), request.getParentId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/comments")
    public ResponseEntity<ApiResponse<PageResponse<InteractionResponse>>> getComments(
            @RequestParam String targetType,
            @RequestParam Long targetId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageResponse<InteractionResponse> response = commentService.getComments(
                targetType, targetId, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/comments/mine")
    public ResponseEntity<ApiResponse<PageResponse<?>>> getMyComments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).body(ApiResponse.error(401, "评论查询需要登录"));
        }

        PageResponse<?> response = commentService.getMyComments(user.getId(), page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/comments/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(@PathVariable Long id) {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).body(ApiResponse.error(401, "需要登录"));
        }

        boolean isAdmin = user.getRole() == Role.ADMIN || user.getRole() == Role.SUPER_ADMIN;
        commentService.deleteComment(id, user.getId(), isAdmin);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // ==================== FAVORITES ====================

    @PostMapping("/favorites")
    public ResponseEntity<ApiResponse<InteractionResponse>> toggleFavorite(
            @Valid @RequestBody InteractionRequest request) {

        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).body(ApiResponse.error(401, "收藏功能需要登录"));
        }

        InteractionResponse response = favoriteService.toggleFavorite(
                request.getTargetType(), request.getTargetId(), user.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/favorites")
    public ResponseEntity<ApiResponse<PageResponse<?>>> getMyFavorites(
            @RequestParam String targetType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).body(ApiResponse.error(401, "收藏功能需要登录"));
        }

        PageResponse<?> response = favoriteService.getMyFavorites(
                targetType, user.getId(), page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/favorites/status")
    public ResponseEntity<ApiResponse<InteractionResponse>> getFavoriteStatus(
            @RequestParam String targetType,
            @RequestParam Long targetId) {

        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).body(ApiResponse.error(401, "收藏功能需要登录"));
        }

        InteractionResponse response = favoriteService.getFavoriteStatus(
                targetType, targetId, user.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ==================== HELPERS ====================

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User) {
            return (User) auth.getPrincipal();
        }
        return null;
    }

    private String computeIpHash(HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        // Support X-Forwarded-For for proxied requests
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            ip = forwarded.split(",")[0].trim();
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(ip.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
