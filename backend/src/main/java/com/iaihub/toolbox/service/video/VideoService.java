package com.iaihub.toolbox.service.video;

import com.iaihub.toolbox.config.VideoStorageConfig;
import com.iaihub.toolbox.dto.PageResponse;
import com.iaihub.toolbox.dto.tag.TagDTO;
import com.iaihub.toolbox.dto.video.VideoListItem;
import com.iaihub.toolbox.dto.video.VideoResponse;
import com.iaihub.toolbox.dto.video.VideoUpdateRequest;
import com.iaihub.toolbox.exception.ForbiddenException;
import com.iaihub.toolbox.exception.ResourceNotFoundException;
import com.iaihub.toolbox.model.Role;
import com.iaihub.toolbox.model.User;
import com.iaihub.toolbox.model.tag.Tag;
import com.iaihub.toolbox.model.tag.TagType;
import com.iaihub.toolbox.model.tag.VideoTag;
import com.iaihub.toolbox.model.video.Video;
import com.iaihub.toolbox.model.video.VideoStatus;
import com.iaihub.toolbox.repository.UserRepository;
import com.iaihub.toolbox.repository.UnifiedLikeRepository;
import com.iaihub.toolbox.repository.UnifiedFavoriteRepository;
import com.iaihub.toolbox.repository.tag.TagRepository;
import com.iaihub.toolbox.repository.tag.VideoTagRepository;
import com.iaihub.toolbox.repository.video.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoService {

    private final VideoRepository videoRepository;
    private final UserRepository userRepository;
    private final UnifiedLikeRepository unifiedLikeRepository;
    private final UnifiedFavoriteRepository unifiedFavoriteRepository;
    private final VideoStorageConfig videoStorageConfig;
    private final TagRepository tagRepository;
    private final VideoTagRepository videoTagRepository;

    /**
     * 5.1 上传视频
     */
    @Transactional
    public Video uploadVideo(MultipartFile file, String title, String description, Long uploaderId, java.util.List<Long> tagIds) {
        // 验证 MP4 格式
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".mp4")) {
            throw new IllegalArgumentException("仅支持 MP4 格式视频");
        }

        // 验证文件大小不超过 1GB
        if (file.getSize() > 1073741824L) {
            throw new IllegalArgumentException("视频文件大小不能超过 1GB");
        }

        // 生成唯一文件名
        String storedFileName = System.currentTimeMillis() + "_" + originalFilename;

        // 先保存文件到磁盘（获取 videoId 前先存文件避免 NOT NULL 约束）
        // 使用临时目录，保存后再移动到最终路径
        Path tempDir = Paths.get(videoStorageConfig.getVideoStoragePath(), "temp");
        Path tempFile = tempDir.resolve(storedFileName);
        try {
            Files.createDirectories(tempDir);
            Files.copy(file.getInputStream(), tempFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("保存视频文件失败", e);
            throw new RuntimeException("保存视频文件失败");
        }

        // 创建实体（file_path 先使用临时路径以满足 NOT NULL 约束）
        Video video = Video.builder()
                .title(title)
                .description(description)
                .fileName(originalFilename)
                .fileSize(file.getSize())
                .filePath("temp/" + storedFileName)
                .uploaderId(uploaderId)
                .build();
        video = videoRepository.save(video);

        // 移动文件到最终目录 uploads/videos/{userId}/{videoId}/original.mp4
        String relativePath = "uploads/videos/" + uploaderId + "/" + video.getId() + "/original.mp4";
        try {
            Path targetDir = Paths.get(videoStorageConfig.getVideoStoragePath(),
                    String.valueOf(uploaderId), String.valueOf(video.getId()));
            Files.createDirectories(targetDir);
            Path targetFile = targetDir.resolve("original.mp4");
            Files.move(tempFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("移动视频文件失败", e);
            throw new RuntimeException("保存视频文件失败");
        }

        // 更新文件路径为最终路径
        video.setFilePath(relativePath);
        video = videoRepository.save(video);

        // Handle tag associations
        if (tagIds != null && !tagIds.isEmpty()) {
            for (Long tagId : tagIds) {
                videoTagRepository.save(new VideoTag(video.getId(), tagId));
                tagRepository.findById(tagId).ifPresent(Tag::incrementUsage);
            }
        }

        return video;
    }

    /**
     * 5.2 获取视频列表
     */
    @Transactional(readOnly = true)
    public PageResponse<VideoListItem> getVideoList(String sortBy, int page, int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        Page<Video> videoPage;

        if ("latest".equalsIgnoreCase(sortBy)) {
            videoPage = videoRepository.findByStatusOrderByCreatedAtDesc(VideoStatus.NORMAL, pageable);
        } else {
            // 默认 hot：pinned DESC, score DESC
            videoPage = videoRepository.findByStatusOrderByHot(VideoStatus.NORMAL, pageable);
        }

        return PageResponse.<VideoListItem>builder()
                .content(videoPage.getContent().stream().map(this::toVideoListItem).toList())
                .totalElements(videoPage.getTotalElements())
                .totalPages(videoPage.getTotalPages())
                .page(page)
                .size(size)
                .build();
    }

    public void pinVideo(Long id) {
        videoRepository.findByIdAndStatus(id, VideoStatus.NORMAL)
                .orElseThrow(() -> new ResourceNotFoundException("视频不存在或已删除"));
        videoRepository.pinById(id);
    }

    public void unpinVideo(Long id) {
        videoRepository.findByIdAndStatus(id, VideoStatus.NORMAL)
                .orElseThrow(() -> new ResourceNotFoundException("视频不存在或已删除"));
        videoRepository.unpinById(id);
    }

    public java.util.List<Long> getHotTop5() {
        return videoRepository.findTop5ByStatusOrderByScoreDesc(PageRequest.of(0, 5));
    }

    /**
     * 5.3 获取视频详情
     */
    @Transactional
    public VideoResponse getVideoDetail(Long id, Long currentUserId) {
        // 观看次数原子 +1（不触发 @PreUpdate，避免 updatedAt 被刷新为当前时间）
        videoRepository.incrementViewCount(id);

        Video video = videoRepository.findByIdAndStatus(id, VideoStatus.NORMAL)
                .orElseThrow(() -> new ResourceNotFoundException("视频不存在或已删除"));

        // 检查用户点赞和收藏状态
        boolean userLiked = false;
        boolean userFavorited = false;
        if (currentUserId != null) {
            userLiked = unifiedLikeRepository.existsByTargetTypeAndTargetIdAndUserId("VIDEO", id, currentUserId);
            userFavorited = unifiedFavoriteRepository.existsByUserIdAndTargetTypeAndTargetId(currentUserId, "VIDEO", id);
        }

        return toVideoResponse(video, userLiked, userFavorited);
    }

    /**
     * 5.4 更新视频信息
     */
    @Transactional
    public VideoResponse updateVideo(Long id, VideoUpdateRequest request, User user) {
        Video video = videoRepository.findByIdAndStatus(id, VideoStatus.NORMAL)
                .orElseThrow(() -> new ResourceNotFoundException("视频不存在或已删除"));

        boolean isOwner = video.getUploaderId().equals(user.getId());
        boolean isAdmin = user.getRole() == Role.ADMIN || user.getRole() == Role.SUPER_ADMIN;
        if (!isOwner && !isAdmin) {
            throw new ForbiddenException("无权操作此内容");
        }

        if (request.getTitle() != null) {
            video.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            video.setDescription(request.getDescription());
        }
        if (request.getDanmakuEnabled() != null) {
            video.setDanmakuEnabled(request.getDanmakuEnabled());
        }

        // Handle tag replacement
        if (request.getTagIds() != null) {
            // Remove old tag associations and decrement usage
            java.util.List<VideoTag> oldTags = videoTagRepository.findByVideoId(id);
            for (VideoTag vt : oldTags) {
                tagRepository.findById(vt.getTagId()).ifPresent(Tag::decrementUsage);
            }
            videoTagRepository.deleteByVideoId(id);

            // Add new tag associations and increment usage
            for (Long tagId : request.getTagIds()) {
                videoTagRepository.save(new VideoTag(id, tagId));
                tagRepository.findById(tagId).ifPresent(Tag::incrementUsage);
            }
        }

        video = videoRepository.save(video);
        return toVideoResponse(video, false, false);
    }

    /**
     * 5.5 删除视频（软删除）
     */
    @Transactional
    public void deleteVideo(Long id, User user) {
        Video video = videoRepository.findByIdAndStatus(id, VideoStatus.NORMAL)
                .orElseThrow(() -> new ResourceNotFoundException("视频不存在或已删除"));

        boolean isOwner = video.getUploaderId().equals(user.getId());
        boolean isAdmin = user.getRole() == Role.ADMIN || user.getRole() == Role.SUPER_ADMIN;
        if (!isOwner && !isAdmin) {
            throw new ForbiddenException("无权操作此内容");
        }

        video.setStatus(VideoStatus.DELETED);
        videoRepository.save(video);
    }

    /**
     * 5.6 获取视频文件的绝对路径（供 Controller 流式播放使用）
     */
    public java.nio.file.Path getVideoFilePath(Long id) {
        Video video = videoRepository.findByIdAndStatus(id, VideoStatus.NORMAL)
                .orElseThrow(() -> new ResourceNotFoundException("视频不存在或已删除"));

        java.nio.file.Path filePath = Paths.get(videoStorageConfig.getUploadBaseDir())
                .resolve(video.getFilePath());
        if (!java.nio.file.Files.exists(filePath)) {
            throw new ResourceNotFoundException("视频文件不存在");
        }
        return filePath;
    }

    /**
     * 5.6b 获取视频流资源（保留兼容）
     */
    public Resource streamVideo(Long id) {
        Video video = videoRepository.findByIdAndStatus(id, VideoStatus.NORMAL)
                .orElseThrow(() -> new ResourceNotFoundException("视频不存在或已删除"));

        try {
            // filePath 格式: uploads/videos/{userId}/{videoId}/original.mp4
            // videoStoragePath 格式: {uploadBaseDir}/uploads/videos
            // 使用 uploadBaseDir + filePath 解析完整路径
            Path filePath = Paths.get(videoStorageConfig.getUploadBaseDir())
                    .resolve(video.getFilePath());
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists()) {
                throw new ResourceNotFoundException("视频文件不存在");
            }
            return resource;
        } catch (MalformedURLException e) {
            log.error("视频文件路径无效", e);
            throw new ResourceNotFoundException("视频文件不存在");
        }
    }

    /**
     * 5.7 获取我上传的视频列表
     */
    @Transactional(readOnly = true)
    public PageResponse<VideoListItem> getMyVideos(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        Page<Video> videoPage = videoRepository.findByUploaderIdAndStatusOrderByCreatedAtDesc(
                userId, VideoStatus.NORMAL, pageable);

        return PageResponse.<VideoListItem>builder()
                .content(videoPage.getContent().stream().map(this::toVideoListItem).toList())
                .totalElements(videoPage.getTotalElements())
                .totalPages(videoPage.getTotalPages())
                .page(page)
                .size(size)
                .build();
    }

    /**
     * 上传视频封面
     */
    @Transactional
    public void uploadCover(Long videoId, Long userId, MultipartFile file) {
        Video video = videoRepository.findByIdAndStatus(videoId, VideoStatus.NORMAL)
                .orElseThrow(() -> new ResourceNotFoundException("视频不存在或已删除"));

        boolean isOwner = video.getUploaderId().equals(userId);
        User user = userRepository.findById(userId).orElse(null);
        boolean isAdmin = user != null && (user.getRole() == Role.ADMIN || user.getRole() == Role.SUPER_ADMIN);
        if (!isOwner && !isAdmin) {
            throw new ForbiddenException("无权操作此内容");
        }

        // Validate file type
        String contentType = file.getContentType();
        if (contentType == null || (!contentType.equals("image/jpeg") && !contentType.equals("image/png"))) {
            throw new IllegalArgumentException("仅支持 JPEG 或 PNG 格式");
        }

        // Validate file size (max 5MB)
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("封面文件大小不能超过 5MB");
        }

        // Save cover file
        String extension = contentType.equals("image/jpeg") ? ".jpg" : ".png";
        String coverFileName = videoId + extension;
        Path coverDir = Paths.get(videoStorageConfig.getUploadBaseDir(), "uploads", "covers");
        try {
            Files.createDirectories(coverDir);
            Path coverFile = coverDir.resolve(coverFileName);
            Files.copy(file.getInputStream(), coverFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("保存封面文件失败", e);
            throw new RuntimeException("保存封面文件失败");
        }

        // Update video coverUrl
        String coverUrl = "/api/v1/videos/" + videoId + "/cover-image";
        video.setCoverUrl(coverUrl);
        videoRepository.save(video);
    }

    /**
     * 获取封面图片路径
     */
    public Path getCoverFilePath(Long videoId) {
        Video video = videoRepository.findByIdAndStatus(videoId, VideoStatus.NORMAL)
                .orElseThrow(() -> new ResourceNotFoundException("视频不存在或已删除"));

        if (video.getCoverUrl() == null || video.getCoverUrl().isBlank()) {
            throw new ResourceNotFoundException("视频未设置封面");
        }

        // Try jpg first, then png
        Path coverDir = Paths.get(videoStorageConfig.getUploadBaseDir(), "uploads", "covers");
        Path jpgPath = coverDir.resolve(videoId + ".jpg");
        Path pngPath = coverDir.resolve(videoId + ".png");

        if (Files.exists(jpgPath)) return jpgPath;
        if (Files.exists(pngPath)) return pngPath;

        throw new ResourceNotFoundException("封面文件不存在");
    }

    /**
     * 将 Video 实体转换为 VideoListItem DTO
     */
    private VideoListItem toVideoListItem(Video video) {
        User uploader = userRepository.findById(video.getUploaderId()).orElse(null);
        String uploaderName = uploader != null ? uploader.getUsername() : "Unknown";
        String uploaderNickname = uploader != null ? uploader.getNickname() : null;
        String uploaderAvatarUrl = uploader != null ? uploader.getAvatarUrl() : null;

        java.util.List<TagDTO> tags = videoTagRepository.findByVideoId(video.getId()).stream()
                .map(vt -> tagRepository.findById(vt.getTagId()).orElse(null))
                .filter(java.util.Objects::nonNull)
                .map(t -> new TagDTO(t.getId(), t.getName(), t.getTagType().name(), t.getUsageCount()))
                .toList();

        return VideoListItem.builder()
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
                .score(video.getScore() != null ? video.getScore() : java.math.BigDecimal.ZERO)
                .pinned(video.getPinned() != null ? video.getPinned() : false)
                .tags(tags)
                .build();
    }

    /**
     * 将 Video 实体转换为 VideoResponse DTO
     */
    private VideoResponse toVideoResponse(Video video, boolean userLiked, boolean userFavorited) {
        User uploader = userRepository.findById(video.getUploaderId()).orElse(null);
        String uploaderName = uploader != null ? uploader.getUsername() : "Unknown";
        String uploaderNickname = uploader != null ? uploader.getNickname() : null;
        String uploaderAvatarUrl = uploader != null ? uploader.getAvatarUrl() : null;

        java.util.List<TagDTO> tags = videoTagRepository.findByVideoId(video.getId()).stream()
                .map(vt -> tagRepository.findById(vt.getTagId()).orElse(null))
                .filter(java.util.Objects::nonNull)
                .map(t -> new TagDTO(t.getId(), t.getName(), t.getTagType().name(), t.getUsageCount()))
                .toList();

        return VideoResponse.builder()
                .id(video.getId())
                .title(video.getTitle())
                .description(video.getDescription())
                .coverUrl(video.getCoverUrl())
                .duration(video.getDuration())
                .fileSize(video.getFileSize())
                .viewCount(video.getViewCount())
                .likeCount(video.getLikeCount())
                .commentCount(video.getCommentCount())
                .uploaderId(video.getUploaderId())
                .uploaderName(uploaderName)
                .uploaderNickname(uploaderNickname)
                .uploaderAvatarUrl(uploaderAvatarUrl)
                .userLiked(userLiked)
                .userFavorited(userFavorited)
                .danmakuEnabled(video.getDanmakuEnabled() != null ? video.getDanmakuEnabled() : true)
                .createdAt(video.getCreatedAt())
                .updatedAt(video.getUpdatedAt())
                .tags(tags)
                .build();
    }
}
