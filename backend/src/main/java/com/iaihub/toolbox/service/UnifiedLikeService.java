package com.iaihub.toolbox.service;

import com.iaihub.toolbox.dto.InteractionResponse;
import com.iaihub.toolbox.dto.PageResponse;
import com.iaihub.toolbox.dto.ToolSummaryDTO;
import com.iaihub.toolbox.dto.video.VideoListItem;
import com.iaihub.toolbox.exception.BusinessException;
import com.iaihub.toolbox.exception.ResourceNotFoundException;
import com.iaihub.toolbox.model.TargetType;
import com.iaihub.toolbox.model.UnifiedLike;
import com.iaihub.toolbox.model.Tool;
import com.iaihub.toolbox.model.User;
import com.iaihub.toolbox.model.forum.ForumPost;
import com.iaihub.toolbox.model.forum.ForumPostStatus;
import com.iaihub.toolbox.model.video.Video;
import com.iaihub.toolbox.model.video.VideoStatus;
import com.iaihub.toolbox.repository.UnifiedLikeRepository;
import com.iaihub.toolbox.repository.ToolRepository;
import com.iaihub.toolbox.repository.UserRepository;
import com.iaihub.toolbox.repository.forum.ForumPostRepository;
import com.iaihub.toolbox.repository.video.VideoRepository;
import com.iaihub.toolbox.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UnifiedLikeService {

    private final UnifiedLikeRepository likeRepository;
    private final ToolRepository toolRepository;
    private final ForumPostRepository forumPostRepository;
    private final VideoRepository videoRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    /**
     * Toggle like for a target resource. Supports both logged-in and anonymous users.
     */
    @Transactional
    public InteractionResponse toggleLike(String targetTypeStr, Long targetId, Long userId, String ipHash) {
        TargetType targetType = TargetType.fromString(targetTypeStr);
        validateTargetExists(targetType, targetId);

        boolean liked;
        int likeCount;

        if (userId != null) {
            // Logged-in user
            Optional<UnifiedLike> existing = likeRepository.findByTargetTypeAndTargetIdAndUserId(
                    targetType.name(), targetId, userId);
            if (existing.isPresent()) {
                likeRepository.deleteByTargetTypeAndTargetIdAndUserId(targetType.name(), targetId, userId);
                liked = false;
            } else {
                UnifiedLike like = UnifiedLike.builder()
                        .targetType(targetType.name())
                        .targetId(targetId)
                        .userId(userId)
                        .build();
                likeRepository.save(like);
                liked = true;
            }
        } else {
            // Anonymous user
            Optional<UnifiedLike> existing = likeRepository.findByTargetTypeAndTargetIdAndIpHash(
                    targetType.name(), targetId, ipHash);
            if (existing.isPresent()) {
                likeRepository.deleteByTargetTypeAndTargetIdAndIpHash(targetType.name(), targetId, ipHash);
                liked = false;
            } else {
                UnifiedLike like = UnifiedLike.builder()
                        .targetType(targetType.name())
                        .targetId(targetId)
                        .ipHash(ipHash)
                        .build();
                likeRepository.save(like);
                liked = true;
            }
        }

        likeCount = updateLikeCount(targetType, targetId, liked);

        // Send LIKE notification to target owner (best-effort, only on like action)
        if (liked && userId != null) {
            try {
                Long ownerId = resolveTargetOwnerId(targetType, targetId);
                if (ownerId != null && !ownerId.equals(userId)) {
                    String actorName = userRepository.findById(userId).map(User::getNickname).orElse(null);
                    notificationService.createLikeNotification(
                            ownerId, targetType.name(), targetId, userId, actorName);
                }
            } catch (Exception e) {
                log.warn("发送点赞通知失败: targetType={}, targetId={}, actorId={}", targetType, targetId, userId, e);
            }
        }

        return InteractionResponse.likeToggle(liked, likeCount);
    }

    /**
     * Get like status for a target resource.
     */
    @Transactional(readOnly = true)
    public InteractionResponse getLikeStatus(String targetTypeStr, Long targetId, Long userId, String ipHash) {
        TargetType targetType = TargetType.fromString(targetTypeStr);

        boolean liked;
        if (userId != null) {
            liked = likeRepository.existsByTargetTypeAndTargetIdAndUserId(targetType.name(), targetId, userId);
        } else if (ipHash != null) {
            liked = likeRepository.existsByTargetTypeAndTargetIdAndIpHash(targetType.name(), targetId, ipHash);
        } else {
            liked = false;
        }

        int likeCount = getLikeCount(targetType, targetId);
        return InteractionResponse.likeToggle(liked, likeCount);
    }

    /**
     * Get the current user's liked resources by target type, returning actual resource DTOs.
     * Mirrors {@link UnifiedFavoriteService#getMyFavorites}; skips soft-deleted targets.
     */
    @Transactional(readOnly = true)
    public PageResponse<?> getMyLikes(String targetTypeStr, Long userId, int page, int size) {
        if (userId == null) {
            throw new BusinessException(401, "点赞查询需要登录");
        }

        TargetType targetType = TargetType.fromString(targetTypeStr);
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        Page<UnifiedLike> likePage = likeRepository
                .findByUserIdAndTargetTypeOrderByCreatedAtDesc(userId, targetType.name(), pageable);

        return switch (targetType) {
            case TOOL -> buildToolLikes(likePage, page, size);
            case FORUM_POST -> buildForumPostLikes(likePage, page, size);
            case VIDEO -> buildVideoLikes(likePage, page, size);
        };
    }

    private PageResponse<ToolSummaryDTO> buildToolLikes(Page<UnifiedLike> likePage, int page, int size) {
        List<ToolSummaryDTO> items = new ArrayList<>();
        for (UnifiedLike like : likePage.getContent()) {
            Optional<Tool> toolOpt = toolRepository.findByIdAndStatusNormal(like.getTargetId());
            if (toolOpt.isPresent()) {
                Tool tool = toolOpt.get();
                Hibernate.initialize(tool.getCategory());
                Hibernate.initialize(tool.getUploader());
                items.add(ToolSummaryDTO.builder()
                        .id(tool.getId())
                        .name(tool.getName())
                        .version(tool.getVersion())
                        .categoryName(tool.getCategory() != null ? tool.getCategory().getName() : null)
                        .categoryIcon(tool.getCategory() != null ? tool.getCategory().getIcon() : null)
                        .uploaderId(tool.getUploader() != null ? tool.getUploader().getId() : null)
                        .uploaderUsername(tool.getUploader() != null ? tool.getUploader().getUsername() : null)
                        .uploaderNickname(tool.getUploader() != null ? tool.getUploader().getNickname() : null)
                        .createdAt(tool.getCreatedAt())
                        .build());
            }
        }
        return PageResponse.<ToolSummaryDTO>builder()
                .content(items)
                .totalElements(likePage.getTotalElements())
                .totalPages(likePage.getTotalPages())
                .page(page)
                .size(size)
                .build();
    }

    private PageResponse<Object> buildForumPostLikes(Page<UnifiedLike> likePage, int page, int size) {
        List<Object> items = new ArrayList<>();
        for (UnifiedLike like : likePage.getContent()) {
            Optional<ForumPost> postOpt = forumPostRepository.findById(like.getTargetId());
            if (postOpt.isPresent() && postOpt.get().getStatus() == ForumPostStatus.NORMAL) {
                ForumPost post = postOpt.get();
                String authorNickname = userRepository.findById(post.getAuthorId())
                        .map(User::getNickname).orElse(null);
                items.add(new ForumPostSummaryDTO(
                        post.getId(), post.getTitle(), post.getAuthorId(), authorNickname,
                        post.getViewCount(), post.getLikeCount(), post.getCommentCount(),
                        post.getCreatedAt()));
            }
        }
        return PageResponse.<Object>builder()
                .content(items)
                .totalElements(likePage.getTotalElements())
                .totalPages(likePage.getTotalPages())
                .page(page)
                .size(size)
                .build();
    }

    private PageResponse<VideoListItem> buildVideoLikes(Page<UnifiedLike> likePage, int page, int size) {
        List<VideoListItem> items = new ArrayList<>();
        for (UnifiedLike like : likePage.getContent()) {
            Optional<Video> videoOpt = videoRepository.findByIdAndStatus(like.getTargetId(), VideoStatus.NORMAL);
            if (videoOpt.isPresent()) {
                Video video = videoOpt.get();
                User uploader = userRepository.findById(video.getUploaderId()).orElse(null);
                items.add(VideoListItem.builder()
                        .id(video.getId())
                        .title(video.getTitle())
                        .coverUrl(video.getCoverUrl())
                        .duration(video.getDuration())
                        .viewCount(video.getViewCount())
                        .likeCount(video.getLikeCount())
                        .commentCount(video.getCommentCount())
                        .uploaderId(video.getUploaderId())
                        .uploaderName(uploader != null ? uploader.getUsername() : "Unknown")
                        .uploaderNickname(uploader != null ? uploader.getNickname() : null)
                        .uploaderAvatarUrl(uploader != null ? uploader.getAvatarUrl() : null)
                        .createdAt(video.getCreatedAt())
                        .build());
            }
        }
        return PageResponse.<VideoListItem>builder()
                .content(items)
                .totalElements(likePage.getTotalElements())
                .totalPages(likePage.getTotalPages())
                .page(page)
                .size(size)
                .build();
    }

    private Long resolveTargetOwnerId(TargetType targetType, Long targetId) {
        return switch (targetType) {
            case TOOL -> toolRepository.findByIdAndStatusNormal(targetId)
                    .map(t -> t.getUploader() != null ? t.getUploader().getId() : null).orElse(null);
            case FORUM_POST -> forumPostRepository.findById(targetId)
                    .map(ForumPost::getAuthorId).orElse(null);
            case VIDEO -> videoRepository.findByIdAndStatus(targetId, VideoStatus.NORMAL)
                    .map(Video::getUploaderId).orElse(null);
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
        }
    }

    private int updateLikeCount(TargetType targetType, Long targetId, boolean liked) {
        switch (targetType) {
            case TOOL -> {
                Tool tool = toolRepository.findByIdAndStatusNormal(targetId).orElseThrow();
                if (liked) {
                    tool.incrementLikeCount();
                } else {
                    tool.decrementLikeCount();
                }
                toolRepository.save(tool);
                return tool.getLikeCount();
            }
            case FORUM_POST -> {
                ForumPost post = forumPostRepository.findById(targetId).orElseThrow();
                if (liked) {
                    post.setLikeCount(post.getLikeCount() + 1);
                } else {
                    post.setLikeCount(Math.max(0, post.getLikeCount() - 1));
                }
                post.updateScore();
                forumPostRepository.save(post);
                return post.getLikeCount();
            }
            case VIDEO -> {
                Video video = videoRepository.findByIdAndStatus(targetId, VideoStatus.NORMAL).orElseThrow();
                if (liked) {
                    video.incrementLikeCount();
                } else {
                    video.decrementLikeCount();
                }
                videoRepository.save(video);
                return video.getLikeCount();
            }
            default -> { return 0; }
        }
    }

    private int getLikeCount(TargetType targetType, Long targetId) {
        return switch (targetType) {
            case TOOL -> toolRepository.findByIdAndStatusNormal(targetId)
                    .map(Tool::getLikeCount).orElse(0);
            case FORUM_POST -> forumPostRepository.findById(targetId)
                    .map(ForumPost::getLikeCount).orElse(0);
            case VIDEO -> videoRepository.findByIdAndStatus(targetId, VideoStatus.NORMAL)
                    .map(Video::getLikeCount).orElse(0);
        };
    }

    /**
     * Lightweight DTO for forum post in "my likes" list.
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ForumPostSummaryDTO {
        private Long id;
        private String title;
        private Long authorId;
        private String authorNickname;
        private Integer viewCount;
        private Integer likeCount;
        private Integer commentCount;
        private java.time.LocalDateTime createdAt;
    }
}
