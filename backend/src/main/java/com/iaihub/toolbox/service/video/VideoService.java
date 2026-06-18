package com.iaihub.toolbox.service.video;

import com.iaihub.toolbox.config.VideoStorageConfig;
import com.iaihub.toolbox.dto.PageResponse;
import com.iaihub.toolbox.dto.video.VideoListItem;
import com.iaihub.toolbox.dto.video.VideoResponse;
import com.iaihub.toolbox.dto.video.VideoUpdateRequest;
import com.iaihub.toolbox.exception.ForbiddenException;
import com.iaihub.toolbox.exception.ResourceNotFoundException;
import com.iaihub.toolbox.model.User;
import com.iaihub.toolbox.model.video.Video;
import com.iaihub.toolbox.model.video.VideoStatus;
import com.iaihub.toolbox.repository.UserRepository;
import com.iaihub.toolbox.repository.video.VideoFavoriteRepository;
import com.iaihub.toolbox.repository.video.VideoLikeRepository;
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
    private final VideoLikeRepository videoLikeRepository;
    private final VideoFavoriteRepository videoFavoriteRepository;
    private final VideoStorageConfig videoStorageConfig;

    /**
     * 5.1 上传视频
     */
    @Transactional
    public Video uploadVideo(MultipartFile file, String title, String description, Long uploaderId) {
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
        return videoRepository.save(video);
    }

    /**
     * 5.2 获取视频列表
     */
    @Transactional(readOnly = true)
    public PageResponse<VideoListItem> getVideoList(int page, int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        Page<Video> videoPage = videoRepository.findByStatusOrderByCreatedAtDesc(VideoStatus.NORMAL, pageable);

        return PageResponse.<VideoListItem>builder()
                .content(videoPage.getContent().stream().map(this::toVideoListItem).toList())
                .totalElements(videoPage.getTotalElements())
                .totalPages(videoPage.getTotalPages())
                .page(page)
                .size(size)
                .build();
    }

    /**
     * 5.3 获取视频详情
     */
    @Transactional
    public VideoResponse getVideoDetail(Long id, Long currentUserId) {
        Video video = videoRepository.findByIdAndStatus(id, VideoStatus.NORMAL)
                .orElseThrow(() -> new ResourceNotFoundException("视频不存在或已删除"));

        // 增加观看次数
        video.incrementViewCount();
        videoRepository.save(video);

        // 检查用户点赞和收藏状态
        boolean userLiked = false;
        boolean userFavorited = false;
        if (currentUserId != null) {
            userLiked = videoLikeRepository.existsByVideoIdAndUserId(id, currentUserId);
            userFavorited = videoFavoriteRepository.existsByVideoIdAndUserId(id, currentUserId);
        }

        return toVideoResponse(video, userLiked, userFavorited);
    }

    /**
     * 5.4 更新视频信息
     */
    @Transactional
    public VideoResponse updateVideo(Long id, VideoUpdateRequest request, Long userId) {
        Video video = videoRepository.findByIdAndStatus(id, VideoStatus.NORMAL)
                .orElseThrow(() -> new ResourceNotFoundException("视频不存在或已删除"));

        if (!video.getUploaderId().equals(userId)) {
            throw new ForbiddenException("您只能编辑自己的视频");
        }

        if (request.getTitle() != null) {
            video.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            video.setDescription(request.getDescription());
        }

        video = videoRepository.save(video);
        return toVideoResponse(video, false, false);
    }

    /**
     * 5.5 删除视频（软删除）
     */
    @Transactional
    public void deleteVideo(Long id, Long userId) {
        Video video = videoRepository.findByIdAndStatus(id, VideoStatus.NORMAL)
                .orElseThrow(() -> new ResourceNotFoundException("视频不存在或已删除"));

        if (!video.getUploaderId().equals(userId)) {
            throw new ForbiddenException("您只能删除自己的视频");
        }

        video.setStatus(VideoStatus.DELETED);
        videoRepository.save(video);
    }

    /**
     * 5.6 获取视频流资源
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
     * 将 Video 实体转换为 VideoListItem DTO
     */
    private VideoListItem toVideoListItem(Video video) {
        User uploader = userRepository.findById(video.getUploaderId()).orElse(null);
        String uploaderName = uploader != null ? uploader.getUsername() : "Unknown";
        String uploaderNickname = uploader != null ? uploader.getNickname() : null;
        String uploaderAvatarUrl = uploader != null ? uploader.getAvatarUrl() : null;

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
                .createdAt(video.getCreatedAt())
                .updatedAt(video.getUpdatedAt())
                .build();
    }
}
