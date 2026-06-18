package com.iaihub.toolbox.service.video;

import com.iaihub.toolbox.dto.PageResponse;
import com.iaihub.toolbox.dto.video.VideoCommentResponse;
import com.iaihub.toolbox.dto.video.VideoInteractionResponse;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VideoInteractionServiceTest {

    @Mock
    private VideoRepository videoRepository;

    @Mock
    private VideoLikeRepository videoLikeRepository;

    @Mock
    private VideoCommentRepository videoCommentRepository;

    @Mock
    private VideoFavoriteRepository videoFavoriteRepository;

    @Mock
    private UserRepository userRepository;

    private VideoInteractionService videoInteractionService;

    private User testUser;
    private Video testVideo;

    @BeforeEach
    void setUp() {
        videoInteractionService = new VideoInteractionService(
                videoRepository,
                videoLikeRepository,
                videoCommentRepository,
                videoFavoriteRepository,
                userRepository
        );

        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .nickname("Test User")
                .password("password")
                .avatarUrl("https://example.com/avatar.jpg")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testVideo = Video.builder()
                .id(1L)
                .title("Test Video")
                .description("A test video")
                .uploaderId(1L)
                .status(VideoStatus.NORMAL)
                .viewCount(10)
                .likeCount(5)
                .commentCount(2)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // ========================================
    // toggleLike tests
    // ========================================

    @Test
    void testToggleLike_Like() {
        // Given — user has NOT liked the video yet
        when(videoRepository.findByIdAndStatus(1L, VideoStatus.NORMAL)).thenReturn(Optional.of(testVideo));
        when(videoLikeRepository.findByVideoIdAndUserId(1L, 2L)).thenReturn(Optional.empty());
        when(videoLikeRepository.save(any(VideoLike.class))).thenAnswer(invocation -> {
            VideoLike like = invocation.getArgument(0);
            like.setId(1L);
            return like;
        });

        // When
        VideoInteractionResponse result = videoInteractionService.toggleLike(1L, 2L);

        // Then
        assertNotNull(result);
        assertTrue(result.getLiked());
        assertEquals(6, result.getLikeCount()); // incremented from 5 to 6
        verify(videoLikeRepository).save(any(VideoLike.class));
        verify(videoLikeRepository, never()).deleteByVideoIdAndUserId(anyLong(), anyLong());
    }

    @Test
    void testToggleLike_Unlike() {
        // Given — user HAS already liked the video
        VideoLike existingLike = VideoLike.builder()
                .id(1L)
                .videoId(1L)
                .userId(2L)
                .createdAt(LocalDateTime.now())
                .build();

        when(videoRepository.findByIdAndStatus(1L, VideoStatus.NORMAL)).thenReturn(Optional.of(testVideo));
        when(videoLikeRepository.findByVideoIdAndUserId(1L, 2L)).thenReturn(Optional.of(existingLike));

        // When
        VideoInteractionResponse result = videoInteractionService.toggleLike(1L, 2L);

        // Then
        assertNotNull(result);
        assertFalse(result.getLiked());
        assertEquals(4, result.getLikeCount()); // decremented from 5 to 4
        verify(videoLikeRepository).deleteByVideoIdAndUserId(1L, 2L);
        verify(videoLikeRepository, never()).save(any(VideoLike.class));
    }

    @Test
    void testToggleLike_VideoNotFound() {
        // Given
        when(videoRepository.findByIdAndStatus(999L, VideoStatus.NORMAL)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () ->
                videoInteractionService.toggleLike(999L, 1L)
        );
    }

    // ========================================
    // addComment tests
    // ========================================

    @Test
    void testAddComment_Success() {
        // Given — plain text content (no XSS to trigger)
        when(videoRepository.findByIdAndStatus(1L, VideoStatus.NORMAL)).thenReturn(Optional.of(testVideo));
        when(videoCommentRepository.save(any(VideoComment.class))).thenAnswer(invocation -> {
            VideoComment comment = invocation.getArgument(0);
            comment.setId(10L);
            comment.setCreatedAt(LocalDateTime.now());
            return comment;
        });
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When
        VideoCommentResponse result = videoInteractionService.addComment(1L, 1L, "Great video!");

        // Then
        assertNotNull(result);
        assertEquals(10L, result.getId());
        assertEquals("Great video!", result.getContent());
        assertEquals(1L, result.getUserId());
        assertEquals("Test User", result.getUserNickname());
        assertEquals("https://example.com/avatar.jpg", result.getUserAvatarUrl());

        // Verify comment count was incremented
        assertEquals(3, testVideo.getCommentCount()); // incremented from 2 to 3
        verify(videoCommentRepository).save(any(VideoComment.class));
        verify(videoRepository).save(testVideo);
    }

    @Test
    void testAddComment_XssSanitized() {
        // Given — content with XSS payload that will be sanitized
        String xssContent = "<script>alert('xss')</script>";

        when(videoRepository.findByIdAndStatus(1L, VideoStatus.NORMAL)).thenReturn(Optional.of(testVideo));
        when(videoCommentRepository.save(any(VideoComment.class))).thenAnswer(invocation -> {
            VideoComment comment = invocation.getArgument(0);
            comment.setId(11L);
            comment.setCreatedAt(LocalDateTime.now());
            return comment;
        });
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When
        VideoCommentResponse result = videoInteractionService.addComment(1L, 1L, xssContent);

        // Then — content should be sanitized (HTML escaped)
        assertNotNull(result);
        assertFalse(result.getContent().contains("<script>"), "XSS content should be escaped");
        assertTrue(result.getContent().contains("&lt;script&gt;"), "Script tags should be HTML-escaped");
    }

    @Test
    void testAddComment_EmptyContent() {
        // Given — empty content

        // When & Then
        assertThrows(IllegalArgumentException.class, () ->
                videoInteractionService.addComment(1L, 1L, "")
        );

        // Verify no comment was saved
        verify(videoCommentRepository, never()).save(any(VideoComment.class));
    }

    @Test
    void testAddComment_BlankContent() {
        // Given — blank content (whitespace only)

        // When & Then
        assertThrows(IllegalArgumentException.class, () ->
                videoInteractionService.addComment(1L, 1L, "   ")
        );
    }

    @Test
    void testAddComment_NullContent() {
        // Given — null content

        // When & Then
        assertThrows(IllegalArgumentException.class, () ->
                videoInteractionService.addComment(1L, 1L, null)
        );
    }

    @Test
    void testAddComment_VideoNotFound() {
        // Given
        when(videoRepository.findByIdAndStatus(999L, VideoStatus.NORMAL)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () ->
                videoInteractionService.addComment(999L, 1L, "Nice!")
        );
    }

    // ========================================
    // toggleFavorite tests
    // ========================================

    @Test
    void testToggleFavorite_Favorite() {
        // Given — user has NOT favorited the video yet
        when(videoRepository.findByIdAndStatus(1L, VideoStatus.NORMAL)).thenReturn(Optional.of(testVideo));
        when(videoFavoriteRepository.findByVideoIdAndUserId(1L, 2L)).thenReturn(Optional.empty());
        when(videoFavoriteRepository.save(any(VideoFavorite.class))).thenAnswer(invocation -> {
            VideoFavorite fav = invocation.getArgument(0);
            fav.setId(1L);
            return fav;
        });

        // When
        VideoInteractionResponse result = videoInteractionService.toggleFavorite(1L, 2L);

        // Then
        assertNotNull(result);
        assertTrue(result.getFavorited());
        verify(videoFavoriteRepository).save(any(VideoFavorite.class));
        verify(videoFavoriteRepository, never()).deleteByVideoIdAndUserId(anyLong(), anyLong());
    }

    @Test
    void testToggleFavorite_Unfavorite() {
        // Given — user HAS already favorited the video
        VideoFavorite existingFavorite = VideoFavorite.builder()
                .id(1L)
                .videoId(1L)
                .userId(2L)
                .createdAt(LocalDateTime.now())
                .build();

        when(videoRepository.findByIdAndStatus(1L, VideoStatus.NORMAL)).thenReturn(Optional.of(testVideo));
        when(videoFavoriteRepository.findByVideoIdAndUserId(1L, 2L)).thenReturn(Optional.of(existingFavorite));

        // When
        VideoInteractionResponse result = videoInteractionService.toggleFavorite(1L, 2L);

        // Then
        assertNotNull(result);
        assertFalse(result.getFavorited());
        verify(videoFavoriteRepository).deleteByVideoIdAndUserId(1L, 2L);
        verify(videoFavoriteRepository, never()).save(any(VideoFavorite.class));
    }

    @Test
    void testToggleFavorite_VideoNotFound() {
        // Given
        when(videoRepository.findByIdAndStatus(999L, VideoStatus.NORMAL)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () ->
                videoInteractionService.toggleFavorite(999L, 1L)
        );
    }

    // ========================================
    // getComments tests
    // ========================================

    @Test
    void testGetComments_Success() {
        // Given
        LocalDateTime commentTime = LocalDateTime.of(2026, 6, 15, 10, 30, 0);

        VideoComment comment1 = VideoComment.builder()
                .id(1L)
                .videoId(1L)
                .userId(1L)
                .content("Great video!")
                .createdAt(commentTime)
                .build();

        VideoComment comment2 = VideoComment.builder()
                .id(2L)
                .videoId(1L)
                .userId(2L)
                .content("Thanks for sharing!")
                .createdAt(commentTime.minusHours(1))
                .build();

        Page<VideoComment> commentPage = new PageImpl<>(List.of(comment1, comment2));

        when(videoCommentRepository.findByVideoIdOrderByCreatedAtDesc(anyLong(), any(Pageable.class)))
                .thenReturn(commentPage);

        User user1 = User.builder()
                .id(1L)
                .username("user1")
                .nickname("User One")
                .avatarUrl("https://example.com/avatar1.jpg")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        User user2 = User.builder()
                .id(2L)
                .username("user2")
                .nickname("User Two")
                .avatarUrl("https://example.com/avatar2.jpg")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));

        // When
        PageResponse<VideoCommentResponse> result = videoInteractionService.getComments(1L, 0, 10);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        assertEquals(2, result.getTotalElements());
        assertEquals(0, result.getPage());
        assertEquals(10, result.getSize());

        VideoCommentResponse first = result.getContent().get(0);
        assertEquals(1L, first.getId());
        assertEquals("Great video!", first.getContent());
        assertEquals(1L, first.getUserId());
        assertEquals("User One", first.getUserNickname());
        assertEquals("https://example.com/avatar1.jpg", first.getUserAvatarUrl());

        VideoCommentResponse second = result.getContent().get(1);
        assertEquals(2L, second.getId());
        assertEquals("Thanks for sharing!", second.getContent());
        assertEquals("User Two", second.getUserNickname());
    }

    @Test
    void testGetComments_EmptyPage() {
        // Given
        Page<VideoComment> emptyPage = new PageImpl<>(List.of());
        when(videoCommentRepository.findByVideoIdOrderByCreatedAtDesc(anyLong(), any(Pageable.class)))
                .thenReturn(emptyPage);

        // When
        PageResponse<VideoCommentResponse> result = videoInteractionService.getComments(1L, 0, 10);

        // Then
        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
        assertEquals(0, result.getTotalElements());
    }

    @Test
    void testGetComments_UserNotFound() {
        // Given — comment exists but user was deleted
        VideoComment comment = VideoComment.builder()
                .id(1L)
                .videoId(1L)
                .userId(999L)
                .content("Orphan comment")
                .createdAt(LocalDateTime.now())
                .build();

        Page<VideoComment> commentPage = new PageImpl<>(List.of(comment));
        when(videoCommentRepository.findByVideoIdOrderByCreatedAtDesc(anyLong(), any(Pageable.class)))
                .thenReturn(commentPage);
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When
        PageResponse<VideoCommentResponse> result = videoInteractionService.getComments(1L, 0, 10);

        // Then — user info should be null
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertNull(result.getContent().get(0).getUserNickname());
        assertNull(result.getContent().get(0).getUserAvatarUrl());
    }
}
