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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VideoServiceTest {

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

    private User testUser;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        videoService = new VideoService(
                videoRepository,
                userRepository,
                videoLikeRepository,
                videoFavoriteRepository,
                videoStorageConfig
        );

        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .nickname("Test User")
                .password("password")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // ========================================
    // uploadVideo tests
    // ========================================

    @Test
    void testUploadVideo_Success() throws IOException {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file", "test-video.mp4", "video/mp4", "fake video content".getBytes());

        when(videoStorageConfig.getVideoStoragePath()).thenReturn(tempDir.toString());

        Video savedVideo = Video.builder()
                .id(1L)
                .title("Test Video")
                .description("A test video")
                .fileName("test-video.mp4")
                .fileSize(file.getSize())
                .uploaderId(1L)
                .status(VideoStatus.NORMAL)
                .viewCount(0)
                .likeCount(0)
                .commentCount(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Video updatedVideo = Video.builder()
                .id(1L)
                .title("Test Video")
                .description("A test video")
                .fileName("test-video.mp4")
                .fileSize(file.getSize())
                .filePath("uploads/videos/1/1/original.mp4")
                .uploaderId(1L)
                .status(VideoStatus.NORMAL)
                .viewCount(0)
                .likeCount(0)
                .commentCount(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(videoRepository.save(any(Video.class)))
                .thenReturn(savedVideo)
                .thenReturn(updatedVideo);

        // When
        Video result = videoService.uploadVideo(file, "Test Video", "A test video", 1L);

        // Then
        assertNotNull(result);
        assertEquals("uploads/videos/1/1/original.mp4", result.getFilePath());
        verify(videoRepository, times(2)).save(any(Video.class));

        // Verify file was actually written to disk
        Path expectedFile = tempDir.resolve("1").resolve("1").resolve("original.mp4");
        assertTrue(Files.exists(expectedFile));
    }

    @Test
    void testUploadVideo_InvalidFormat() {
        // Given — a non-MP4 file
        MockMultipartFile file = new MockMultipartFile(
                "file", "test-video.avi", "video/avi", "fake video content".getBytes());

        // When & Then
        assertThrows(IllegalArgumentException.class, () ->
                videoService.uploadVideo(file, "Test Video", "A test video", 1L)
        );

        // Verify no save happened
        verify(videoRepository, never()).save(any(Video.class));
    }

    @Test
    void testUploadVideo_NullFilename() {
        // Given — null filename
        MockMultipartFile file = new MockMultipartFile(
                "file", null, "video/mp4", "fake video content".getBytes());

        // When & Then
        assertThrows(IllegalArgumentException.class, () ->
                videoService.uploadVideo(file, "Test Video", "A test video", 1L)
        );
    }

    @Test
    void testUploadVideo_FileTooLarge() {
        // Given — a file larger than 1GB (1073741824 bytes)
        long oversizedSize = 1073741825L;
        MockMultipartFile file = new MockMultipartFile(
                "file", "huge-video.mp4", "video/mp4", new byte[0]) {
            @Override
            public long getSize() {
                return oversizedSize;
            }
        };

        // When & Then
        assertThrows(IllegalArgumentException.class, () ->
                videoService.uploadVideo(file, "Huge Video", "A huge video", 1L)
        );

        // Verify no save happened
        verify(videoRepository, never()).save(any(Video.class));
    }

    // ========================================
    // getVideoList tests
    // ========================================

    @Test
    void testGetVideoList_Success() {
        // Given
        Video video = Video.builder()
                .id(1L)
                .title("Test Video")
                .coverUrl("/covers/1.jpg")
                .duration(120)
                .viewCount(10)
                .likeCount(5)
                .commentCount(3)
                .uploaderId(1L)
                .status(VideoStatus.NORMAL)
                .createdAt(LocalDateTime.now())
                .build();

        Page<Video> videoPage = new PageImpl<>(List.of(video));

        when(videoRepository.findByStatusOrderByCreatedAtDesc(any(VideoStatus.class), any(Pageable.class)))
                .thenReturn(videoPage);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When
        PageResponse<VideoListItem> result = videoService.getVideoList(0, 10);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("Test Video", result.getContent().get(0).getTitle());
        assertEquals("testuser", result.getContent().get(0).getUploaderName());
        assertEquals("Test User", result.getContent().get(0).getUploaderNickname());
        assertEquals(1, result.getTotalElements());
        assertEquals(0, result.getPage());
        assertEquals(10, result.getSize());
    }

    @Test
    void testGetVideoList_EmptyPage() {
        // Given
        Page<Video> emptyPage = new PageImpl<>(List.of());
        when(videoRepository.findByStatusOrderByCreatedAtDesc(any(VideoStatus.class), any(Pageable.class)))
                .thenReturn(emptyPage);

        // When
        PageResponse<VideoListItem> result = videoService.getVideoList(0, 10);

        // Then
        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
        assertEquals(0, result.getTotalElements());
    }

    // ========================================
    // getVideoDetail tests
    // ========================================

    @Test
    void testGetVideoDetail_Success() {
        // Given
        Video video = Video.builder()
                .id(1L)
                .title("Test Video")
                .description("A test video")
                .coverUrl("/covers/1.jpg")
                .duration(120)
                .fileSize(1024L)
                .viewCount(5)
                .likeCount(3)
                .commentCount(2)
                .uploaderId(1L)
                .status(VideoStatus.NORMAL)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(videoRepository.findByIdAndStatus(1L, VideoStatus.NORMAL)).thenReturn(Optional.of(video));
        when(videoRepository.save(any(Video.class))).thenReturn(video);
        when(videoLikeRepository.existsByVideoIdAndUserId(1L, 2L)).thenReturn(true);
        when(videoFavoriteRepository.existsByVideoIdAndUserId(1L, 2L)).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When
        VideoResponse result = videoService.getVideoDetail(1L, 2L);

        // Then
        assertNotNull(result);
        assertEquals("Test Video", result.getTitle());
        assertEquals("A test video", result.getDescription());
        assertEquals(6, result.getViewCount()); // viewCount incremented from 5 to 6
        assertTrue(result.getUserLiked());
        assertFalse(result.getUserFavorited());
        assertEquals("testuser", result.getUploaderName());
        assertEquals("Test User", result.getUploaderNickname());

        // Verify viewCount was persisted
        verify(videoRepository).save(video);
    }

    @Test
    void testGetVideoDetail_NotFound() {
        // Given — video is deleted (returns empty)
        when(videoRepository.findByIdAndStatus(999L, VideoStatus.NORMAL)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () ->
                videoService.getVideoDetail(999L, 1L)
        );
    }

    @Test
    void testGetVideoDetail_NullCurrentUser() {
        // Given — no authenticated user (currentUserId is null)
        Video video = Video.builder()
                .id(1L)
                .title("Test Video")
                .description("A test video")
                .uploaderId(1L)
                .status(VideoStatus.NORMAL)
                .viewCount(0)
                .likeCount(0)
                .commentCount(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(videoRepository.findByIdAndStatus(1L, VideoStatus.NORMAL)).thenReturn(Optional.of(video));
        when(videoRepository.save(any(Video.class))).thenReturn(video);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When
        VideoResponse result = videoService.getVideoDetail(1L, null);

        // Then
        assertNotNull(result);
        assertFalse(result.getUserLiked());
        assertFalse(result.getUserFavorited());
        // Verify like/favorite checks were NOT performed for null user
        verify(videoLikeRepository, never()).existsByVideoIdAndUserId(anyLong(), anyLong());
        verify(videoFavoriteRepository, never()).existsByVideoIdAndUserId(anyLong(), anyLong());
    }

    // ========================================
    // updateVideo tests
    // ========================================

    @Test
    void testUpdateVideo_Success() {
        // Given
        Video video = Video.builder()
                .id(1L)
                .title("Old Title")
                .description("Old description")
                .uploaderId(1L)
                .status(VideoStatus.NORMAL)
                .viewCount(0)
                .likeCount(0)
                .commentCount(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        VideoUpdateRequest request = VideoUpdateRequest.builder()
                .title("New Title")
                .description("New description")
                .build();

        when(videoRepository.findByIdAndStatus(1L, VideoStatus.NORMAL)).thenReturn(Optional.of(video));
        when(videoRepository.save(any(Video.class))).thenReturn(video);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When
        VideoResponse result = videoService.updateVideo(1L, request, 1L);

        // Then
        assertNotNull(result);
        assertEquals("New Title", video.getTitle());
        assertEquals("New description", video.getDescription());
        verify(videoRepository).save(video);
    }

    @Test
    void testUpdateVideo_Forbidden() {
        // Given — user 2 tries to update user 1's video
        Video video = Video.builder()
                .id(1L)
                .title("Someone Else's Video")
                .description("Description")
                .uploaderId(1L)  // owned by user 1
                .status(VideoStatus.NORMAL)
                .build();

        VideoUpdateRequest request = VideoUpdateRequest.builder()
                .title("Hacked Title")
                .description("Hacked description")
                .build();

        when(videoRepository.findByIdAndStatus(1L, VideoStatus.NORMAL)).thenReturn(Optional.of(video));

        // When & Then — user 2 tries to update
        assertThrows(ForbiddenException.class, () ->
                videoService.updateVideo(1L, request, 2L)
        );

        // Verify save was NOT called
        verify(videoRepository, never()).save(any(Video.class));
    }

    @Test
    void testUpdateVideo_NotFound() {
        // Given
        VideoUpdateRequest request = VideoUpdateRequest.builder()
                .title("Updated Title")
                .build();

        when(videoRepository.findByIdAndStatus(999L, VideoStatus.NORMAL)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () ->
                videoService.updateVideo(999L, request, 1L)
        );
    }

    // ========================================
    // deleteVideo tests
    // ========================================

    @Test
    void testDeleteVideo_Success() {
        // Given
        Video video = Video.builder()
                .id(1L)
                .title("Video to Delete")
                .uploaderId(1L)
                .status(VideoStatus.NORMAL)
                .build();

        when(videoRepository.findByIdAndStatus(1L, VideoStatus.NORMAL)).thenReturn(Optional.of(video));

        // When
        videoService.deleteVideo(1L, 1L);

        // Then
        assertEquals(VideoStatus.DELETED, video.getStatus());
        verify(videoRepository).save(video);
    }

    @Test
    void testDeleteVideo_Forbidden() {
        // Given — user 2 tries to delete user 1's video
        Video video = Video.builder()
                .id(1L)
                .title("Someone Else's Video")
                .uploaderId(1L)  // owned by user 1
                .status(VideoStatus.NORMAL)
                .build();

        when(videoRepository.findByIdAndStatus(1L, VideoStatus.NORMAL)).thenReturn(Optional.of(video));

        // When & Then — user 2 tries to delete
        assertThrows(ForbiddenException.class, () ->
                videoService.deleteVideo(1L, 2L)
        );

        // Verify status was NOT changed
        assertEquals(VideoStatus.NORMAL, video.getStatus());
    }

    @Test
    void testDeleteVideo_NotFound() {
        // Given
        when(videoRepository.findByIdAndStatus(999L, VideoStatus.NORMAL)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () ->
                videoService.deleteVideo(999L, 1L)
        );
    }
}
