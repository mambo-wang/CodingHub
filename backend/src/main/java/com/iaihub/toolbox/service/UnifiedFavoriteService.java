package com.iaihub.toolbox.service;

import com.iaihub.toolbox.dto.*;
import com.iaihub.toolbox.dto.video.VideoListItem;
import com.iaihub.toolbox.exception.BusinessException;
import com.iaihub.toolbox.exception.ResourceNotFoundException;
import com.iaihub.toolbox.model.*;
import com.iaihub.toolbox.model.forum.ForumPost;
import com.iaihub.toolbox.model.forum.ForumPostStatus;
import com.iaihub.toolbox.model.video.Video;
import com.iaihub.toolbox.model.video.VideoStatus;
import com.iaihub.toolbox.repository.UnifiedFavoriteRepository;
import com.iaihub.toolbox.repository.ToolRepository;
import com.iaihub.toolbox.repository.UserRepository;
import com.iaihub.toolbox.repository.forum.ForumPostRepository;
import com.iaihub.toolbox.repository.video.VideoRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
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
public class UnifiedFavoriteService {

    private final UnifiedFavoriteRepository favoriteRepository;
    private final ToolRepository toolRepository;
    private final ForumPostRepository forumPostRepository;
    private final VideoRepository videoRepository;
    private final UserRepository userRepository;

    /**
     * Toggle favorite for a target resource. Requires logged-in user.
     */
    @Transactional
    public InteractionResponse toggleFavorite(String targetTypeStr, Long targetId, Long userId) {
        if (userId == null) {
            throw new BusinessException(401, "收藏功能需要登录");
        }

        TargetType targetType = TargetType.fromString(targetTypeStr);
        validateTargetExists(targetType, targetId);

        Optional<UnifiedFavorite> existing = favoriteRepository.findByUserIdAndTargetTypeAndTargetId(
                userId, targetType.name(), targetId);

        boolean favorited;
        if (existing.isPresent()) {
            favoriteRepository.deleteByUserIdAndTargetTypeAndTargetId(userId, targetType.name(), targetId);
            favorited = false;
        } else {
            UnifiedFavorite favorite = UnifiedFavorite.builder()
                    .targetType(targetType.name())
                    .targetId(targetId)
                    .userId(userId)
                    .build();
            favoriteRepository.save(favorite);
            favorited = true;
        }

        // Update tool-level denormalized counter and hot score
        if (targetType == TargetType.TOOL) {
            toolRepository.findByIdAndStatusNormal(targetId).ifPresent(tool -> {
                if (favorited) {
                    tool.incrementFavoriteCount();
                } else {
                    tool.decrementFavoriteCount();
                }
                toolRepository.save(tool);
            });
        }

        return InteractionResponse.favoriteToggle(favorited);
    }

    /**
     * Get user's favorites by target type, returning the actual resource DTOs.
     */
    @Transactional(readOnly = true)
    public PageResponse<?> getMyFavorites(String targetTypeStr, Long userId, int page, int size) {
        if (userId == null) {
            throw new BusinessException(401, "收藏功能需要登录");
        }

        TargetType targetType = TargetType.fromString(targetTypeStr);
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        Page<UnifiedFavorite> favoritePage = favoriteRepository
                .findByUserIdAndTargetTypeOrderByCreatedAtDesc(userId, targetType.name(), pageable);

        return switch (targetType) {
            case TOOL -> buildToolFavorites(favoritePage, page, size);
            case FORUM_POST -> buildForumPostFavorites(favoritePage, page, size);
            case VIDEO -> buildVideoFavorites(favoritePage, page, size);
        };
    }

    /**
     * Get favorite status for a specific target.
     */
    @Transactional(readOnly = true)
    public InteractionResponse getFavoriteStatus(String targetTypeStr, Long targetId, Long userId) {
        if (userId == null) {
            throw new BusinessException(401, "收藏功能需要登录");
        }

        TargetType targetType = TargetType.fromString(targetTypeStr);
        boolean favorited = favoriteRepository.existsByUserIdAndTargetTypeAndTargetId(
                userId, targetType.name(), targetId);

        return InteractionResponse.favoriteToggle(favorited);
    }

    private PageResponse<ToolSummaryDTO> buildToolFavorites(Page<UnifiedFavorite> favoritePage, int page, int size) {
        List<ToolSummaryDTO> items = new ArrayList<>();
        for (UnifiedFavorite fav : favoritePage.getContent()) {
            Optional<Tool> toolOpt = toolRepository.findByIdAndStatusNormal(fav.getTargetId());
            if (toolOpt.isPresent()) {
                Tool tool = toolOpt.get();
                Hibernate.initialize(tool.getCategory());
                Hibernate.initialize(tool.getUploader());
                items.add(ToolSummaryDTO.builder()
                        .id(tool.getId())
                        .name(tool.getName())
                        .version(tool.getVersion())
                        .categoryName(tool.getCategory().getName())
                        .categoryIcon(tool.getCategory().getIcon())
                        .uploaderId(tool.getUploader().getId())
                        .uploaderUsername(tool.getUploader().getUsername())
                        .uploaderNickname(tool.getUploader().getNickname())
                        .createdAt(tool.getCreatedAt())
                        .build());
            }
        }
        return PageResponse.<ToolSummaryDTO>builder()
                .content(items)
                .totalElements(favoritePage.getTotalElements())
                .totalPages(favoritePage.getTotalPages())
                .page(page)
                .size(size)
                .build();
    }

    private PageResponse<Object> buildForumPostFavorites(Page<UnifiedFavorite> favoritePage, int page, int size) {
        List<Object> items = new ArrayList<>();
        for (UnifiedFavorite fav : favoritePage.getContent()) {
            Optional<ForumPost> postOpt = forumPostRepository.findById(fav.getTargetId());
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
                .totalElements(favoritePage.getTotalElements())
                .totalPages(favoritePage.getTotalPages())
                .page(page)
                .size(size)
                .build();
    }

    private PageResponse<VideoListItem> buildVideoFavorites(Page<UnifiedFavorite> favoritePage, int page, int size) {
        List<VideoListItem> items = new ArrayList<>();
        for (UnifiedFavorite fav : favoritePage.getContent()) {
            Optional<Video> videoOpt = videoRepository.findByIdAndStatus(fav.getTargetId(), VideoStatus.NORMAL);
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
                .totalElements(favoritePage.getTotalElements())
                .totalPages(favoritePage.getTotalPages())
                .page(page)
                .size(size)
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

    /**
     * Lightweight DTO for forum post in favorites list.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
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
