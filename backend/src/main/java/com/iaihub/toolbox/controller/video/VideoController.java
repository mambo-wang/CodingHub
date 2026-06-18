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
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

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
     * 视频流播放（支持 HTTP Range）
     */
    @GetMapping("/{id}/stream")
    public ResponseEntity<InputStreamResource> streamVideo(
            @PathVariable Long id,
            HttpServletRequest request) throws IOException {

        Resource resource = videoService.streamVideo(id);
        String contentType = "video/mp4";
        String rangeHeader = request.getHeader("Range");

        if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
            long fileLength = resource.contentLength();
            String[] ranges = rangeHeader.substring(6).split("-");
            long rangeStart = Long.parseLong(ranges[0]);
            long rangeEnd = ranges.length > 1 && !ranges[1].isEmpty()
                    ? Long.parseLong(ranges[1])
                    : fileLength - 1;

            if (rangeStart >= fileLength) {
                return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                        .header("Content-Range", "bytes */" + fileLength)
                        .build();
            }

            long contentLength = rangeEnd - rangeStart + 1;
            InputStreamResource inputStreamResource = new InputStreamResource(resource.getInputStream());

            return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                    .contentType(MediaType.parseMediaType(contentType))
                    .contentLength(contentLength)
                    .header("Content-Range", "bytes " + rangeStart + "-" + rangeEnd + "/" + fileLength)
                    .header("Accept-Ranges", "bytes")
                    .body(inputStreamResource);
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .contentLength(resource.contentLength())
                .header("Accept-Ranges", "bytes")
                .body(new InputStreamResource(resource.getInputStream()));
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
