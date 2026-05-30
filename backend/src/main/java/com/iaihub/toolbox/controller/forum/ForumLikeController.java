package com.iaihub.toolbox.controller.forum;

import com.iaihub.toolbox.dto.forum.ForumLikeRequest;
import com.iaihub.toolbox.service.forum.ForumLikeService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.iaihub.toolbox.model.User;
import org.springframework.web.bind.annotation.*;

import java.security.MessageDigest;
import java.util.HexFormat;

@RestController
@RequestMapping("/api/forum/likes")
@RequiredArgsConstructor
public class ForumLikeController {

    private final ForumLikeService likeService;

    @PostMapping
    public ResponseEntity<Void> like(
            @RequestBody ForumLikeRequest request,
            @AuthenticationPrincipal User user,
            HttpServletRequest httpRequest) {

        Long userId = user != null ? user.getId() : null;
        String ipHash = user == null ? hashIp(httpRequest.getRemoteAddr()) : null;

        if (request.postId() != null) {
            likeService.likePost(request.postId(), userId, ipHash);
        } else {
            likeService.likeComment(request.commentId(), userId, ipHash);
        }

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping
    public ResponseEntity<Void> unlike(
            @RequestBody ForumLikeRequest request,
            @AuthenticationPrincipal User user,
            HttpServletRequest httpRequest) {

        Long userId = user != null ? user.getId() : null;
        String ipHash = user == null ? hashIp(httpRequest.getRemoteAddr()) : null;

        if (request.postId() != null) {
            likeService.unlikePost(request.postId(), userId, ipHash);
        }

        return ResponseEntity.noContent().build();
    }

    private String hashIp(String ip) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(ip.getBytes());
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            return ip;
        }
    }
}