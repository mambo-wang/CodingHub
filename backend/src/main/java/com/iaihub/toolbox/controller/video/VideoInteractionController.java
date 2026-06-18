package com.iaihub.toolbox.controller.video;

import com.iaihub.toolbox.dto.ApiResponse;
import com.iaihub.toolbox.dto.PageResponse;
import com.iaihub.toolbox.dto.video.VideoCommentRequest;
import com.iaihub.toolbox.dto.video.VideoCommentResponse;
import com.iaihub.toolbox.dto.video.VideoInteractionResponse;
import com.iaihub.toolbox.dto.video.VideoListItem;
import com.iaihub.toolbox.model.User;
import com.iaihub.toolbox.service.video.VideoInteractionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/videos")
@RequiredArgsConstructor
public class VideoInteractionController {

    private final VideoInteractionService videoInteractionService;

    /**
     * 切换点赞状态
     */
    @PostMapping("/{id}/like")
    public ResponseEntity<ApiResponse<VideoInteractionResponse>> toggleLike(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {

        VideoInteractionResponse response = videoInteractionService.toggleLike(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 切换收藏状态
     */
    @PostMapping("/{id}/favorite")
    public ResponseEntity<ApiResponse<VideoInteractionResponse>> toggleFavorite(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {

        VideoInteractionResponse response = videoInteractionService.toggleFavorite(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 获取视频评论列表（分页）
     */
    @GetMapping("/{id}/comments")
    public ResponseEntity<ApiResponse<PageResponse<VideoCommentResponse>>> getComments(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageResponse<VideoCommentResponse> response = videoInteractionService.getComments(id, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 添加评论
     */
    @PostMapping("/{id}/comments")
    public ResponseEntity<ApiResponse<VideoCommentResponse>> addComment(
            @PathVariable Long id,
            @Valid @RequestBody VideoCommentRequest request,
            @AuthenticationPrincipal User currentUser) {

        VideoCommentResponse response = videoInteractionService.addComment(
                id, currentUser.getId(), request.getContent());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("评论成功", response));
    }

    /**
     * 获取我的收藏列表
     */
    @GetMapping("/my/favorites")
    public ResponseEntity<ApiResponse<PageResponse<VideoListItem>>> getMyFavorites(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageResponse<VideoListItem> response = videoInteractionService.getMyFavorites(
                currentUser.getId(), page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
