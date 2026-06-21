package com.iaihub.toolbox.service;

import com.iaihub.toolbox.dto.InteractionResponse;
import com.iaihub.toolbox.exception.ResourceNotFoundException;
import com.iaihub.toolbox.model.TargetType;
import com.iaihub.toolbox.model.UnifiedLike;
import com.iaihub.toolbox.model.Tool;
import com.iaihub.toolbox.model.forum.ForumPost;
import com.iaihub.toolbox.model.forum.ForumPostStatus;
import com.iaihub.toolbox.model.video.Video;
import com.iaihub.toolbox.model.video.VideoStatus;
import com.iaihub.toolbox.repository.UnifiedLikeRepository;
import com.iaihub.toolbox.repository.ToolRepository;
import com.iaihub.toolbox.repository.forum.ForumPostRepository;
import com.iaihub.toolbox.repository.video.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UnifiedLikeService {

    private final UnifiedLikeRepository likeRepository;
    private final ToolRepository toolRepository;
    private final ForumPostRepository forumPostRepository;
    private final VideoRepository videoRepository;

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
}
