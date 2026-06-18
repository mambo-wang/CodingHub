package com.iaihub.toolbox.service.video;

import com.iaihub.toolbox.config.VideoStorageConfig;
import com.iaihub.toolbox.exception.ResourceNotFoundException;
import com.iaihub.toolbox.model.video.Video;
import com.iaihub.toolbox.model.video.VideoStatus;
import com.iaihub.toolbox.repository.UserRepository;
import com.iaihub.toolbox.repository.video.VideoFavoriteRepository;
import com.iaihub.toolbox.repository.video.VideoLikeRepository;
import com.iaihub.toolbox.repository.video.VideoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for the streamVideo method of VideoService.
 *
 * HTTP Range request handling is typically tested via integration tests with a running server,
 * because the actual Range header processing happens in the Spring MVC controller layer
 * (ResourceHttpMessageConverter). These unit tests verify that the service layer correctly
 * resolves and returns the Resource for a given video, which is a prerequisite for
 * HTTP Range support at the controller level.
 *
 * Integration test notes for HTTP Range:
 * - GET /api/videos/{id}/stream with header "Range: bytes=0-1023" should return 206 Partial Content
 * - The response should include "Content-Range: bytes 0-1023/{totalSize}"
 * - The response should include "Accept-Ranges: bytes"
 * - A request without Range header should return 200 OK with the full content
 */
@ExtendWith(MockitoExtension.class)
class VideoStreamTest {

    @Mock
    private VideoRepository videoRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private VideoLikeRepository videoLikeRepository;

    @Mock
    private VideoFavoriteRepository videoFavoriteRepository;

    @Mock
    private VideoStorageConfig videoStorageConfig;

    private VideoService videoService;

    @TempDir
    Path tempDir;

    private Video testVideo;

    @BeforeEach
    void setUp() {
        videoService = new VideoService(
                videoRepository,
                userRepository,
                videoLikeRepository,
                videoFavoriteRepository,
                videoStorageConfig
        );

        testVideo = Video.builder()
                .id(1L)
                .title("Test Video")
                .description("A test video")
                .filePath("uploads/videos/1/1/original.mp4")
                .fileName("original.mp4")
                .fileSize(1024L)
                .uploaderId(1L)
                .status(VideoStatus.NORMAL)
                .viewCount(0)
                .likeCount(0)
                .commentCount(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // ========================================
    // streamVideo tests
    // ========================================

    @Test
    void testStreamVideo_Success() throws IOException {
        // Given — create the actual file on disk at the expected path
        Path videoDir = tempDir.resolve("uploads").resolve("videos").resolve("1").resolve("1");
        Files.createDirectories(videoDir);
        Path videoFile = videoDir.resolve("original.mp4");
        Files.write(videoFile, "fake video bytes for streaming test".getBytes());

        when(videoRepository.findByIdAndStatus(1L, VideoStatus.NORMAL)).thenReturn(Optional.of(testVideo));
        when(videoStorageConfig.getUploadBaseDir()).thenReturn(tempDir.toString());

        // When
        Resource result = videoService.streamVideo(1L);

        // Then
        assertNotNull(result);
        assertTrue(result.exists(), "Resource should exist on disk");
        assertTrue(result.isReadable(), "Resource should be readable");
        assertTrue(result.contentLength() > 0, "Resource should have content");
    }

    @Test
    void testStreamVideo_VideoNotFound() {
        // Given — video does not exist in DB
        when(videoRepository.findByIdAndStatus(999L, VideoStatus.NORMAL)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () ->
                videoService.streamVideo(999L)
        );
    }

    @Test
    void testStreamVideo_FileMissing() {
        // Given — video exists in DB but the file is missing on disk
        // (no file created under tempDir)
        when(videoRepository.findByIdAndStatus(1L, VideoStatus.NORMAL)).thenReturn(Optional.of(testVideo));
        when(videoStorageConfig.getUploadBaseDir()).thenReturn(tempDir.toString());

        // When & Then — the file does not exist, so ResourceNotFoundException is expected
        assertThrows(ResourceNotFoundException.class, () ->
                videoService.streamVideo(1L)
        );
    }

    @Test
    void testStreamVideo_DeletedVideo() {
        // Given — video was soft-deleted, findByIdAndStatus returns empty for NORMAL
        when(videoRepository.findByIdAndStatus(1L, VideoStatus.NORMAL)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () ->
                videoService.streamVideo(1L)
        );

        // Verify no config access was attempted
        verify(videoStorageConfig, never()).getUploadBaseDir();
    }

    @Test
    void testStreamVideo_InvalidPath() {
        // Given — video has an invalid file path that causes MalformedURLException
        Video badPathVideo = Video.builder()
                .id(2L)
                .title("Bad Path Video")
                .filePath(":::invalid-path:::")
                .fileName("original.mp4")
                .fileSize(1024L)
                .uploaderId(1L)
                .status(VideoStatus.NORMAL)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(videoRepository.findByIdAndStatus(2L, VideoStatus.NORMAL)).thenReturn(Optional.of(badPathVideo));
        when(videoStorageConfig.getUploadBaseDir()).thenReturn(tempDir.toString());

        // When & Then — MalformedURLException is caught and re-thrown as ResourceNotFoundException
        assertThrows(ResourceNotFoundException.class, () ->
                videoService.streamVideo(2L)
        );
    }
}
