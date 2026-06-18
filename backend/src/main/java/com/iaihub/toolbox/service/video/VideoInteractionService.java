package com.iaihub.toolbox.service.video;

import com.iaihub.toolbox.dto.PageResponse;
import com.iaihub.toolbox.dto.video.VideoCommentResponse;
import com.iaihub.toolbox.dto.video.VideoInteractionResponse;
import com.iaihub.toolbox.dto.video.VideoListItem;
import com.iaihub.toolbox.exception.ResourceNotFoundException;
import com.iaihub.toolbox.model.User;
import com.iaihub.toolbox.model.video.Video;
import com.iaihub.toolbox.model.video.VideoComment;
import com.iaihub.toolbox.model.video.VideoFavorite;
import com.iaihub.toolbox.model.video.VideoLike;
import com.iaihub.toolbox.model.video.VideoStatus;
import com.iaihub.toolbox.repository.UserRepository;
import com.iaihub.toolbox.repository.video.VideoCommentRepository;
import com.iaihub.toolbox.repository.video.VideoFavoriteRepository;
import com.iaihub.toolbox.repository.video.VideoLikeRepository;
import com.iaihub.toolbox.repository.video.VideoRepository;
import com.iaihub.toolbox.util.XssSanitizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
public class VideoInteractionService {

    private final VideoRepository videoRepository;
    private final VideoLikeRepository videoLikeRepository;
    private final VideoCommentRepository videoCommentRepository;
    private final VideoFavoriteRepository videoFavoriteRepository;
    private final UserRepository userRepository;

    /**
     * 6.1 切换点赞状态（toggle）
     */
    @Transactional
    public VideoInteractionResponse toggleLike(Long videoId, Long userId) {
        Video video = videoRepository.findByIdAndStatus(videoId, VideoStatus.NORMAL)
                .orElseThrow(() -> new ResourceNotFoundException("视频不存在或已删除"));

        Optional<VideoLike> existingLike = videoLikeRepository.findByVideoIdAndUserId(videoId, userId);
        boolean liked;

        if (existingLike.isPresent()) {
            // 已点赞，取消点赞
            videoLikeRepository.deleteByVideoIdAndUserId(videoId, userId);
            video.decrementLikeCount();
            liked = false;
        } else {
            // 未点赞，添加点赞
            VideoLike like = VideoLike.builder()
                    .videoId(videoId)
                    .userId(userId)
                    .build();
            videoLikeRepository.save(like);
            video.incrementLikeCount();
            liked = true;
        }

        videoRepository.save(video);

        return VideoInteractionResponse.builder()
                .liked(liked)
                .likeCount(video.getLikeCount())
                .build();
    }

    /**
     * 6.2 添加评论
     */
    @Transactional
    public VideoCommentResponse addComment(Long videoId, Long userId, String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("评论内容不能为空");
        }

        Video video = videoRepository.findByIdAndStatus(videoId, VideoStatus.NORMAL)
                .orElseThrow(() -> new ResourceNotFoundException("视频不存在或已删除"));

        // XSS 过滤
        String sanitizedContent = XssSanitizer.sanitize(content);

        VideoComment comment = VideoComment.builder()
                .videoId(videoId)
                .userId(userId)
                .content(sanitizedContent)
                .build();
        comment = videoCommentRepository.save(comment);

        // 更新视频评论数
        video.incrementCommentCount();
        videoRepository.save(video);

        // 获取用户信息
        User user = userRepository.findById(userId).orElse(null);
        String userNickname = user != null ? user.getNickname() : null;
        String userAvatarUrl = user != null ? user.getAvatarUrl() : null;

        return VideoCommentResponse.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .userId(comment.getUserId())
                .userNickname(userNickname)
                .userAvatarUrl(userAvatarUrl)
                .createdAt(comment.getCreatedAt())
                .build();
    }

    /**
     * 6.3 获取评论列表（分页）
     */
    @Transactional(readOnly = true)
    public PageResponse<VideoCommentResponse> getComments(Long videoId, int page, int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        Page<VideoComment> commentPage = videoCommentRepository.findByVideoIdOrderByCreatedAtDesc(videoId, pageable);

        List<VideoCommentResponse> comments = commentPage.getContent().stream()
                .map(comment -> {
                    User user = userRepository.findById(comment.getUserId()).orElse(null);
                    String userNickname = user != null ? user.getNickname() : null;
                    String userAvatarUrl = user != null ? user.getAvatarUrl() : null;

                    return VideoCommentResponse.builder()
                            .id(comment.getId())
                            .content(comment.getContent())
                            .userId(comment.getUserId())
                            .userNickname(userNickname)
                            .userAvatarUrl(userAvatarUrl)
                            .createdAt(comment.getCreatedAt())
                            .build();
                })
                .toList();

        return PageResponse.<VideoCommentResponse>builder()
                .content(comments)
                .totalElements(commentPage.getTotalElements())
                .totalPages(commentPage.getTotalPages())
                .page(page)
                .size(size)
                .build();
    }

    /**
     * 6.4 切换收藏状态（toggle）
     */
    @Transactional
    public VideoInteractionResponse toggleFavorite(Long videoId, Long userId) {
        Video video = videoRepository.findByIdAndStatus(videoId, VideoStatus.NORMAL)
                .orElseThrow(() -> new ResourceNotFoundException("视频不存在或已删除"));

        Optional<VideoFavorite> existingFavorite = videoFavoriteRepository.findByVideoIdAndUserId(videoId, userId);
        boolean favorited;

        if (existingFavorite.isPresent()) {
            // 已收藏，取消收藏
            videoFavoriteRepository.deleteByVideoIdAndUserId(videoId, userId);
            favorited = false;
        } else {
            // 未收藏，添加收藏
            VideoFavorite favorite = VideoFavorite.builder()
                    .videoId(videoId)
                    .userId(userId)
                    .build();
            videoFavoriteRepository.save(favorite);
            favorited = true;
        }

        return VideoInteractionResponse.builder()
                .favorited(favorited)
                .build();
    }

    /**
     * 6.5 获取我的收藏列表（分页）
     */
    @Transactional(readOnly = true)
    public PageResponse<VideoListItem> getMyFavorites(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        Page<VideoFavorite> favoritePage = videoFavoriteRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);

        List<VideoListItem> items = new ArrayList<>();
        for (VideoFavorite favorite : favoritePage.getContent()) {
            Optional<Video> videoOpt = videoRepository.findByIdAndStatus(favorite.getVideoId(), VideoStatus.NORMAL);
            if (videoOpt.isPresent()) {
                Video video = videoOpt.get();
                User uploader = userRepository.findById(video.getUploaderId()).orElse(null);
                String uploaderName = uploader != null ? uploader.getUsername() : "Unknown";
                String uploaderNickname = uploader != null ? uploader.getNickname() : null;
                String uploaderAvatarUrl = uploader != null ? uploader.getAvatarUrl() : null;

                items.add(VideoListItem.builder()
                        .id(video.getId())
                        .title(video.getTitle())
                        .coverUrl(video.getCoverUrl())
                        .duration(video.getDuration())
                        .viewCount(video.getViewCount())
                        .likeCount(video.getLikeCount())
                        .commentCount(video.getCommentCount())
                        .uploaderId(video.getUploaderId())
                        .uploaderName(uploaderName)
                        .uploaderNickname(uploaderNickname)
                        .uploaderAvatarUrl(uploaderAvatarUrl)
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
}
