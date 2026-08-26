package com.iaihub.toolbox.service;

import com.iaihub.toolbox.dto.InteractionResponse;
import com.iaihub.toolbox.dto.PageResponse;
import com.iaihub.toolbox.exception.BusinessException;
import com.iaihub.toolbox.exception.ForbiddenException;
import com.iaihub.toolbox.exception.ResourceNotFoundException;
import com.iaihub.toolbox.model.*;
import com.iaihub.toolbox.model.forum.ForumPost;
import com.iaihub.toolbox.model.forum.ForumPostStatus;
import com.iaihub.toolbox.model.video.Video;
import com.iaihub.toolbox.model.video.VideoStatus;
import com.iaihub.toolbox.repository.PluginRepository;
import com.iaihub.toolbox.repository.UnifiedCommentRepository;
import com.iaihub.toolbox.repository.ToolRepository;
import com.iaihub.toolbox.repository.UserRepository;
import com.iaihub.toolbox.repository.forum.ForumPostRepository;
import com.iaihub.toolbox.repository.video.VideoRepository;
import com.iaihub.toolbox.service.notification.NotificationService;
import com.iaihub.toolbox.util.XssSanitizer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UnifiedCommentService {

    private final UnifiedCommentRepository commentRepository;
    private final ToolRepository toolRepository;
    private final ForumPostRepository forumPostRepository;
    private final VideoRepository videoRepository;
    private final PluginRepository pluginRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    /**
     * Add a comment (top-level or nested reply). Supports logged-in and anonymous users.
     */
    @Transactional
    public InteractionResponse addComment(String targetTypeStr, Long targetId,
                                           Long userId, String userName,
                                           String content, Long parentId) {
        if (content == null || content.isBlank()) {
            throw new BusinessException(400, "评论内容不能为空");
        }

        TargetType targetType = TargetType.fromString(targetTypeStr);
        validateTargetExists(targetType, targetId);

        String sanitizedContent = XssSanitizer.sanitize(content);

        // Resolve parent/root for nested replies
        Long resolvedParentId = null;
        Long resolvedRootId = null;
        if (parentId != null) {
            UnifiedComment parent = commentRepository.findById(parentId)
                    .orElseThrow(() -> new ResourceNotFoundException("父评论不存在"));
            resolvedParentId = parentId;
            resolvedRootId = parent.getRootId() != null ? parent.getRootId() : parentId;
        }

        // Resolve user display name
        String resolvedUserName = userName;
        String userNickname = null;
        String userAvatarUrl = null;
        if (userId != null) {
            User user = userRepository.findById(userId).orElse(null);
            if (user != null) {
                userNickname = user.getNickname();
                userAvatarUrl = user.getAvatarUrl();
                resolvedUserName = null; // Logged-in users don't need userName field
            }
        }

        UnifiedComment comment = UnifiedComment.builder()
                .targetType(targetType.name())
                .targetId(targetId)
                .userId(userId)
                .userName(resolvedUserName)
                .parentId(resolvedParentId)
                .rootId(resolvedRootId)
                .content(sanitizedContent)
                .build();
        comment = commentRepository.save(comment);

        // Update commentCount on target
        incrementCommentCount(targetType, targetId);

        // Send COMMENT_REPLY notification to target owner (best-effort)
        if (userId != null) {
            try {
                Long ownerId = resolveTargetOwnerId(targetType, targetId);
                if (ownerId != null && !ownerId.equals(userId)) {
                    String actorName = userNickname != null ? userNickname : "用户";
                    notificationService.createCommentNotification(
                            ownerId, targetType.name(), targetId, userId, actorName, sanitizedContent);
                }
            } catch (Exception e) {
                log.warn("发送评论通知失败: targetType={}, targetId={}, actorId={}", targetType, targetId, userId, e);
            }
        }

        return InteractionResponse.builder()
                .id(comment.getId())
                .targetType(comment.getTargetType())
                .targetId(comment.getTargetId())
                .userId(comment.getUserId())
                .userName(comment.getUserName())
                .userNickname(userNickname)
                .userAvatarUrl(userAvatarUrl)
                .parentId(comment.getParentId())
                .rootId(comment.getRootId())
                .content(comment.getContent())
                .commentLikeCount(comment.getLikeCount())
                .createdAt(comment.getCreatedAt())
                .build();
    }

    /**
     * Get paginated comments for a target resource.
     */
    @Transactional(readOnly = true)
    public PageResponse<InteractionResponse> getComments(String targetTypeStr, Long targetId,
                                                          int page, int size) {
        TargetType.fromString(targetTypeStr); // validate

        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        Page<UnifiedComment> commentPage = commentRepository
                .findByTargetTypeAndTargetIdOrderByCreatedAtAsc(targetTypeStr.toUpperCase(), targetId, pageable);

        List<InteractionResponse> responses = commentPage.getContent().stream()
                .map(this::toResponse)
                .toList();

        return PageResponse.<InteractionResponse>builder()
                .content(responses)
                .totalElements(commentPage.getTotalElements())
                .totalPages(commentPage.getTotalPages())
                .page(page)
                .size(size)
                .build();
    }

    /**
     * Get the current user's comments, newest first, with the target title resolved
     * by type. Comments whose target has been soft-deleted are skipped.
     */
    @Transactional(readOnly = true)
    public PageResponse<MyCommentDTO> getMyComments(Long userId, int page, int size) {
        if (userId == null) {
            throw new BusinessException(401, "评论查询需要登录");
        }

        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        Page<UnifiedComment> commentPage = commentRepository
                .findByUserIdOrderByCreatedAtDesc(userId, pageable);

        List<MyCommentDTO> items = new ArrayList<>();
        for (UnifiedComment comment : commentPage.getContent()) {
            String targetTitle = resolveTargetTitle(comment.getTargetType(), comment.getTargetId());
            if (targetTitle == null) {
                continue; // target soft-deleted, skip to avoid dead link
            }
            items.add(MyCommentDTO.builder()
                    .id(comment.getId())
                    .targetType(comment.getTargetType())
                    .targetId(comment.getTargetId())
                    .targetTitle(targetTitle)
                    .content(comment.getContent())
                    .createdAt(comment.getCreatedAt())
                    .build());
        }

        return PageResponse.<MyCommentDTO>builder()
                .content(items)
                .totalElements(commentPage.getTotalElements())
                .totalPages(commentPage.getTotalPages())
                .page(page)
                .size(size)
                .build();
    }

    private String resolveTargetTitle(String targetTypeStr, Long targetId) {
        return switch (TargetType.fromString(targetTypeStr)) {
            case TOOL -> toolRepository.findByIdAndStatusNormal(targetId).map(Tool::getName).orElse(null);
            case FORUM_POST -> forumPostRepository.findById(targetId)
                    .filter(p -> p.getStatus() == ForumPostStatus.NORMAL)
                    .map(ForumPost::getTitle).orElse(null);
            case VIDEO -> videoRepository.findByIdAndStatus(targetId, VideoStatus.NORMAL)
                    .map(Video::getTitle).orElse(null);
            case PLUGIN -> pluginRepository.findByIdAndStatusNormal(targetId)
                    .map(Plugin::getName).orElse(null);
        };
    }

    /**
     * Delete a comment with owner/admin permission check.
     */
    @Transactional
    public void deleteComment(Long commentId, Long userId, boolean isAdmin) {
        UnifiedComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("评论不存在"));

        boolean isOwner = comment.getUserId() != null && comment.getUserId().equals(userId);
        if (!isOwner && !isAdmin) {
            throw new ForbiddenException("无权删除此评论");
        }

        TargetType targetType = TargetType.fromString(comment.getTargetType());
        commentRepository.delete(comment);
        decrementCommentCount(targetType, comment.getTargetId());
    }

    private InteractionResponse toResponse(UnifiedComment comment) {
        String userNickname = null;
        String userAvatarUrl = null;
        if (comment.getUserId() != null) {
            User user = userRepository.findById(comment.getUserId()).orElse(null);
            if (user != null) {
                userNickname = user.getNickname();
                userAvatarUrl = user.getAvatarUrl();
            }
        }
        return InteractionResponse.builder()
                .id(comment.getId())
                .targetType(comment.getTargetType())
                .targetId(comment.getTargetId())
                .userId(comment.getUserId())
                .userName(comment.getUserName())
                .userNickname(userNickname)
                .userAvatarUrl(userAvatarUrl)
                .parentId(comment.getParentId())
                .rootId(comment.getRootId())
                .content(comment.getContent())
                .commentLikeCount(comment.getLikeCount())
                .createdAt(comment.getCreatedAt())
                .build();
    }

    private Long resolveTargetOwnerId(TargetType targetType, Long targetId) {
        return switch (targetType) {
            case TOOL -> toolRepository.findByIdAndStatusNormal(targetId)
                    .map(t -> t.getUploader() != null ? t.getUploader().getId() : null)
                    .orElse(null);
            case FORUM_POST -> forumPostRepository.findById(targetId)
                    .map(ForumPost::getAuthorId).orElse(null);
            case VIDEO -> videoRepository.findByIdAndStatus(targetId, VideoStatus.NORMAL)
                    .map(Video::getUploaderId).orElse(null);
            case PLUGIN -> pluginRepository.findByIdAndStatusNormal(targetId)
                    .map(p -> p.getAuthor() != null ? p.getAuthor().getId() : null)
                    .orElse(null);
        };
    }

    private void validateTargetExists(TargetType targetType, Long targetId) {
        switch (targetType) {
            case TOOL -> toolRepository.findByIdAndStatusNormal(targetId)
                    .orElseThrow(() -> new ResourceNotFoundException("工具不存在或已删除"));
            case FORUM_POST -> forumPostRepository.findById(targetId)
                    .filter(p -> p.getStatus() == ForumPostStatus.NORMAL)
                    .orElseThrow(() -> new ResourceNotFoundException("帖子不存在或已删除"));
            case VIDEO -> videoRepository.findByIdAndStatus(targetId, VideoStatus.NORMAL)
                    .orElseThrow(() -> new ResourceNotFoundException("视频不存在或已删除"));
            case PLUGIN -> pluginRepository.findByIdAndStatusNormal(targetId)
                    .orElseThrow(() -> new ResourceNotFoundException("插件不存在或已删除"));
        }
    }

    private void incrementCommentCount(TargetType targetType, Long targetId) {
        switch (targetType) {
            case TOOL -> toolRepository.incrementCommentCount(targetId);
            case FORUM_POST -> forumPostRepository.incrementCommentCount(targetId);
            case VIDEO -> videoRepository.incrementCommentCount(targetId);
            case PLUGIN -> pluginRepository.incrementCommentCount(targetId);
        }
    }

    private void decrementCommentCount(TargetType targetType, Long targetId) {
        switch (targetType) {
            case TOOL -> toolRepository.decrementCommentCount(targetId);
            case FORUM_POST -> forumPostRepository.decrementCommentCount(targetId);
            case VIDEO -> videoRepository.decrementCommentCount(targetId);
            case PLUGIN -> pluginRepository.decrementCommentCount(targetId);
        }
    }

    /**
     * DTO for a user's comment in "my comments" list.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MyCommentDTO {
        private Long id;
        private String targetType;
        private Long targetId;
        private String targetTitle;
        private String content;
        private java.time.LocalDateTime createdAt;
    }
}
