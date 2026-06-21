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
import com.iaihub.toolbox.repository.UnifiedCommentRepository;
import com.iaihub.toolbox.repository.ToolRepository;
import com.iaihub.toolbox.repository.UserRepository;
import com.iaihub.toolbox.repository.forum.ForumPostRepository;
import com.iaihub.toolbox.repository.video.VideoRepository;
import com.iaihub.toolbox.util.XssSanitizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UnifiedCommentService {

    private final UnifiedCommentRepository commentRepository;
    private final ToolRepository toolRepository;
    private final ForumPostRepository forumPostRepository;
    private final VideoRepository videoRepository;
    private final UserRepository userRepository;

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

    private void validateTargetExists(TargetType targetType, Long targetId) {
        switch (targetType) {
            case TOOL -> toolRepository.findByIdAndStatusNormal(targetId)
                    .orElseThrow(() -> new ResourceNotFoundException("工具不存在或已删除"));
            case FORUM_POST -> forumPostRepository.findById(targetId)
                    .filter(p -> p.getStatus() == ForumPostStatus.NORMAL)
                    .orElseThrow(() -> new ResourceNotFoundException("帖子不存在或已删除"));
            case VIDEO -> videoRepository.findByIdAndStatus(targetId, VideoStatus.NORMAL)
                    .orElseThrow(() -> new ResourceNotFoundException("视频不存在或已删除"));
        }
    }

    private void incrementCommentCount(TargetType targetType, Long targetId) {
        switch (targetType) {
            case TOOL -> {
                Tool tool = toolRepository.findByIdAndStatusNormal(targetId).orElseThrow();
                tool.incrementCommentCount();
                toolRepository.save(tool);
            }
            case FORUM_POST -> {
                ForumPost post = forumPostRepository.findById(targetId).orElseThrow();
                post.setCommentCount(post.getCommentCount() + 1);
                forumPostRepository.save(post);
            }
            case VIDEO -> {
                Video video = videoRepository.findByIdAndStatus(targetId, VideoStatus.NORMAL).orElseThrow();
                video.incrementCommentCount();
                videoRepository.save(video);
            }
        }
    }

    private void decrementCommentCount(TargetType targetType, Long targetId) {
        switch (targetType) {
            case TOOL -> {
                Tool tool = toolRepository.findByIdAndStatusNormal(targetId).orElse(null);
                if (tool != null) {
                    tool.setCommentCount(Math.max(0, tool.getCommentCount() - 1));
                    tool.updateScore();
                    toolRepository.save(tool);
                }
            }
            case FORUM_POST -> {
                ForumPost post = forumPostRepository.findById(targetId).orElse(null);
                if (post != null) {
                    post.setCommentCount(Math.max(0, post.getCommentCount() - 1));
                    post.updateScore();
                    forumPostRepository.save(post);
                }
            }
            case VIDEO -> {
                Video video = videoRepository.findByIdAndStatus(targetId, VideoStatus.NORMAL).orElse(null);
                if (video != null) {
                    video.setCommentCount(Math.max(0, video.getCommentCount() - 1));
                    videoRepository.save(video);
                }
            }
        }
    }
}
