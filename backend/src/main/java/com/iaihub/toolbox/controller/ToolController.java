package com.iaihub.toolbox.controller;

import com.iaihub.toolbox.dto.*;
import com.iaihub.toolbox.model.User;
import com.iaihub.toolbox.service.ToolService;
import java.util.List;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tools")
@RequiredArgsConstructor
public class ToolController {

    private final ToolService toolService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ToolSummaryDTO>>> getTools(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "latest") String sortBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {

        PageResponse<ToolSummaryDTO> response = toolService.getTools(categoryId, keyword, sortBy, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ToolDetailDTO>> getToolById(@PathVariable Long id) {
        ToolDetailDTO tool = toolService.getToolById(id);
        return ResponseEntity.ok(ApiResponse.success(tool));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ToolSummaryDTO>> createTool(
            @Valid @RequestBody CreateToolRequest request,
            @AuthenticationPrincipal User currentUser) {

        ToolSummaryDTO tool = toolService.createTool(request, currentUser.getId());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created("上传成功", tool));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ToolDetailDTO>> updateTool(
            @PathVariable Long id,
            @Valid @RequestBody UpdateToolRequest request,
            @AuthenticationPrincipal User currentUser) {

        ToolDetailDTO tool = toolService.updateTool(id, request, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("更新成功", tool));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTool(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {

        toolService.deleteTool(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("删除成功", null));
    }

    @PostMapping("/{id}/like")
    public ResponseEntity<ApiResponse<Void>> likeTool(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {

        toolService.likeTool(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("点赞成功", null));
    }

    @DeleteMapping("/{id}/like")
    public ResponseEntity<ApiResponse<Void>> unlikeTool(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {

        toolService.unlikeTool(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("取消点赞成功", null));
    }

    @GetMapping("/{id}/like-status")
    public ResponseEntity<ApiResponse<Boolean>> getLikeStatus(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {

        boolean isLiked = toolService.isLikedByUser(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(isLiked));
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<ApiResponse<ToolCommentDto>> addComment(
            @PathVariable Long id,
            @Valid @RequestBody CreateCommentRequest request,
            @AuthenticationPrincipal User currentUser) {

        ToolCommentDto comment = toolService.addComment(id, currentUser.getId(), request.getContent());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("评论成功", comment));
    }

    @GetMapping("/{id}/comments")
    public ResponseEntity<ApiResponse<List<ToolCommentDto>>> getComments(@PathVariable Long id) {
        List<ToolCommentDto> comments = toolService.getComments(id);
        return ResponseEntity.ok(ApiResponse.success(comments));
    }
}
