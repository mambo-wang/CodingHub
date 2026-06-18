package com.iaihub.toolbox.controller.video;

import com.iaihub.toolbox.dto.ApiResponse;
import com.iaihub.toolbox.dto.PageResponse;
import com.iaihub.toolbox.dto.video.VideoListItem;
import com.iaihub.toolbox.dto.video.VideoResponse;
import com.iaihub.toolbox.dto.video.VideoUpdateRequest;
import com.iaihub.toolbox.model.User;
import com.iaihub.toolbox.model.video.Video;
import com.iaihub.toolbox.service.video.VideoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.file.Path;

@Slf4j
@RestController
@RequestMapping("/api/v1/videos")
@RequiredArgsConstructor
public class VideoController {

    private final VideoService videoService;

    /**
     * 上传视频
     */
    @PostMapping
    public ResponseEntity<ApiResponse<VideoResponse>> uploadVideo(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false) String description,
            @AuthenticationPrincipal User currentUser) {

        Video video = videoService.uploadVideo(file, title, description, currentUser.getId());

        VideoResponse response = VideoResponse.builder()
                .id(video.getId())
                .title(video.getTitle())
                .description(video.getDescription())
                .fileSize(video.getFileSize())
                .uploaderId(video.getUploaderId())
                .createdAt(video.getCreatedAt())
                .updatedAt(video.getUpdatedAt())
                .build();

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created("上传成功", response));
    }

    /**
     * 获取视频列表
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<VideoListItem>>> getVideoList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageResponse<VideoListItem> response = videoService.getVideoList(page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 获取视频详情
     * currentUser may be null for unauthenticated access
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<VideoResponse>> getVideoDetail(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {

        Long userId = currentUser != null ? currentUser.getId() : null;
        VideoResponse response = videoService.getVideoDetail(id, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 更新视频信息
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<VideoResponse>> updateVideo(
            @PathVariable Long id,
            @Valid @RequestBody VideoUpdateRequest request,
            @AuthenticationPrincipal User currentUser) {

        VideoResponse response = videoService.updateVideo(id, request, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("更新成功", response));
    }

    /**
     * 删除视频
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteVideo(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {

        videoService.deleteVideo(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("删除成功", null));
    }

    /**
     * 视频流播放（支持 HTTP Range，使用 RandomAccessFile 精确 seek）
     */
    @GetMapping("/{id}/stream")
    public void streamVideo(
            @PathVariable Long id,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        Path filePath = videoService.getVideoFilePath(id);
        long fileLength = filePath.toFile().length();
        String contentType = "video/mp4";

        response.setContentType(contentType);
        response.setHeader("Accept-Ranges", "bytes");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");

        String rangeHeader = request.getHeader("Range");

        if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
            String[] ranges = rangeHeader.substring(6).split("-");
            long rangeStart = Long.parseLong(ranges[0]);
            long rangeEnd = ranges.length > 1 && !ranges[1].isEmpty()
                    ? Long.parseLong(ranges[1])
                    : Math.min(rangeStart + 1024 * 1024 - 1, fileLength - 1); // 默认每次最多 1MB

            if (rangeStart >= fileLength) {
                response.setStatus(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                response.setHeader("Content-Range", "bytes */" + fileLength);
                return;
            }

            if (rangeEnd >= fileLength) {
                rangeEnd = fileLength - 1;
            }

            long contentLength = rangeEnd - rangeStart + 1;

            response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
            response.setHeader("Content-Range", "bytes " + rangeStart + "-" + rangeEnd + "/" + fileLength);
            response.setContentLengthLong(contentLength);

            try (RandomAccessFile raf = new RandomAccessFile(filePath.toFile(), "r");
                 OutputStream os = response.getOutputStream()) {
                raf.seek(rangeStart);
                byte[] buffer = new byte[8192];
                long remaining = contentLength;
                int read;
                while (remaining > 0 && (read = raf.read(buffer, 0, (int) Math.min(buffer.length, remaining))) != -1) {
                    os.write(buffer, 0, read);
                    remaining -= read;
                }
                os.flush();
            }
        } else {
            // Full file response
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentLengthLong(fileLength);

            try (RandomAccessFile raf = new RandomAccessFile(filePath.toFile(), "r");
                 OutputStream os = response.getOutputStream()) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = raf.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
                os.flush();
            }
        }
    }

    /**
     * 获取我上传的视频列表
     */
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<PageResponse<VideoListItem>>> getMyVideos(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageResponse<VideoListItem> response = videoService.getMyVideos(currentUser.getId(), page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
