package com.iaihub.toolbox.controller;

import com.iaihub.toolbox.dto.ApiResponse;
import com.iaihub.toolbox.model.PostFavorite;
import com.iaihub.toolbox.util.JwtUtil;
import com.iaihub.toolbox.service.PostFavoriteService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/post-favorites")
@RequiredArgsConstructor
public class PostFavoriteController {

    private final PostFavoriteService service;
    private final JwtUtil jwtUtil;

    @PostMapping("/{postId}")
    public ResponseEntity<ApiResponse<PostFavorite>> addFavorite(
            @PathVariable Long postId,
            HttpServletRequest request) {
        Long userId = getUserId(request);
        PostFavorite favorite = service.addFavorite(userId, postId);
        return ResponseEntity.ok(ApiResponse.success(favorite));
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<ApiResponse<Boolean>> removeFavorite(
            @PathVariable Long postId,
            HttpServletRequest request) {
        Long userId = getUserId(request);
        boolean success = service.removeFavorite(userId, postId);
        return ResponseEntity.ok(ApiResponse.success(success));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PostFavorite>>> getUserFavorites(HttpServletRequest request) {
        Long userId = getUserId(request);
        List<PostFavorite> favorites = service.getUserFavorites(userId);
        return ResponseEntity.ok(ApiResponse.success(favorites));
    }

    @GetMapping("/check/{postId}")
    public ResponseEntity<ApiResponse<Boolean>> checkFavorite(
            @PathVariable Long postId,
            HttpServletRequest request) {
        Long userId = getUserId(request);
        boolean isFavorited = service.isFavorited(userId, postId);
        return ResponseEntity.ok(ApiResponse.success(isFavorited));
    }

    private Long getUserId(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        String token = authHeader.substring(7);
        return jwtUtil.getUserIdFromToken(token);
    }
}
