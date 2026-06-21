package com.iaihub.toolbox.service;

import com.iaihub.toolbox.dto.InteractionResponse;
import com.iaihub.toolbox.exception.BusinessException;
import com.iaihub.toolbox.exception.ResourceNotFoundException;
import com.iaihub.toolbox.model.Tool;
import com.iaihub.toolbox.model.UnifiedLike;
import com.iaihub.toolbox.model.forum.ForumPost;
import com.iaihub.toolbox.model.forum.ForumPostStatus;
import com.iaihub.toolbox.model.video.Video;
import com.iaihub.toolbox.model.video.VideoStatus;
import com.iaihub.toolbox.repository.UnifiedLikeRepository;
import com.iaihub.toolbox.repository.ToolRepository;
import com.iaihub.toolbox.repository.forum.ForumPostRepository;
import com.iaihub.toolbox.repository.video.VideoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UnifiedLikeServiceTest {

    @Mock
    private UnifiedLikeRepository likeRepository;

    @Mock
    private ToolRepository toolRepository;

    @Mock
    private ForumPostRepository forumPostRepository;

    @Mock
    private VideoRepository videoRepository;

    private UnifiedLikeService likeService;

    private Tool testTool;
    private ForumPost testPost;
    private Video testVideo;

    @BeforeEach
    void setUp() {
        likeService = new UnifiedLikeService(likeRepository, toolRepository, forumPostRepository, videoRepository);

        testTool = Tool.builder()
                .id(1L)
                .name("Test Tool")
                .likeCount(5)
                .status(Tool.Status.NORMAL)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testPost = ForumPost.builder()
                .id(10L)
                .title("Test Post")
                .likeCount(8)
                .status(ForumPostStatus.NORMAL)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testVideo = Video.builder()
                .id(20L)
                .title("Test Video")
                .likeCount(3)
                .status(VideoStatus.NORMAL)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // ==================== Logged-in user ====================

    @Test
    void toggleLike_loggedInUser_likeTool() {
        when(toolRepository.findByIdAndStatusNormal(1L)).thenReturn(Optional.of(testTool));
        when(likeRepository.findByTargetTypeAndTargetIdAndUserId("TOOL", 1L, 100L))
                .thenReturn(Optional.empty());
        when(likeRepository.save(any(UnifiedLike.class))).thenAnswer(inv -> {
            UnifiedLike like = inv.getArgument(0);
            like.setId(1L);
            return like;
        });

        InteractionResponse response = likeService.toggleLike("TOOL", 1L, 100L, null);

        assertTrue(response.getLiked());
        assertEquals(6, response.getLikeCount());
        verify(likeRepository).save(any(UnifiedLike.class));
    }

    @Test
    void toggleLike_loggedInUser_unlikeTool() {
        UnifiedLike existing = UnifiedLike.builder().id(1L).targetType("TOOL").targetId(1L).userId(100L).build();
        when(toolRepository.findByIdAndStatusNormal(1L)).thenReturn(Optional.of(testTool));
        when(likeRepository.findByTargetTypeAndTargetIdAndUserId("TOOL", 1L, 100L))
                .thenReturn(Optional.of(existing));

        InteractionResponse response = likeService.toggleLike("TOOL", 1L, 100L, null);

        assertFalse(response.getLiked());
        assertEquals(4, response.getLikeCount());
        verify(likeRepository).deleteByTargetTypeAndTargetIdAndUserId("TOOL", 1L, 100L);
    }

    @Test
    void toggleLike_loggedInUser_likeForumPost() {
        when(forumPostRepository.findById(10L)).thenReturn(Optional.of(testPost));
        when(likeRepository.findByTargetTypeAndTargetIdAndUserId("FORUM_POST", 10L, 100L))
                .thenReturn(Optional.empty());
        when(likeRepository.save(any(UnifiedLike.class))).thenAnswer(inv -> inv.getArgument(0));

        InteractionResponse response = likeService.toggleLike("FORUM_POST", 10L, 100L, null);

        assertTrue(response.getLiked());
        assertEquals(9, response.getLikeCount());
    }

    @Test
    void toggleLike_loggedInUser_likeVideo() {
        when(videoRepository.findByIdAndStatus(20L, VideoStatus.NORMAL)).thenReturn(Optional.of(testVideo));
        when(likeRepository.findByTargetTypeAndTargetIdAndUserId("VIDEO", 20L, 100L))
                .thenReturn(Optional.empty());
        when(likeRepository.save(any(UnifiedLike.class))).thenAnswer(inv -> inv.getArgument(0));

        InteractionResponse response = likeService.toggleLike("VIDEO", 20L, 100L, null);

        assertTrue(response.getLiked());
        assertEquals(4, response.getLikeCount());
    }

    // ==================== Anonymous user ====================

    @Test
    void toggleLike_anonymousUser_likeTool() {
        String ipHash = "abc123hash";
        when(toolRepository.findByIdAndStatusNormal(1L)).thenReturn(Optional.of(testTool));
        when(likeRepository.findByTargetTypeAndTargetIdAndIpHash("TOOL", 1L, ipHash))
                .thenReturn(Optional.empty());
        when(likeRepository.save(any(UnifiedLike.class))).thenAnswer(inv -> inv.getArgument(0));

        InteractionResponse response = likeService.toggleLike("TOOL", 1L, null, ipHash);

        assertTrue(response.getLiked());
        assertEquals(6, response.getLikeCount());
    }

    @Test
    void toggleLike_anonymousUser_unlikeTool() {
        String ipHash = "abc123hash";
        UnifiedLike existing = UnifiedLike.builder().id(1L).targetType("TOOL").targetId(1L).ipHash(ipHash).build();
        when(toolRepository.findByIdAndStatusNormal(1L)).thenReturn(Optional.of(testTool));
        when(likeRepository.findByTargetTypeAndTargetIdAndIpHash("TOOL", 1L, ipHash))
                .thenReturn(Optional.of(existing));

        InteractionResponse response = likeService.toggleLike("TOOL", 1L, null, ipHash);

        assertFalse(response.getLiked());
        assertEquals(4, response.getLikeCount());
    }

    // ==================== Resource not found ====================

    @Test
    void toggleLike_toolNotFound() {
        when(toolRepository.findByIdAndStatusNormal(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                likeService.toggleLike("TOOL", 999L, 100L, null));
    }

    @Test
    void toggleLike_forumPostNotFound() {
        when(forumPostRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                likeService.toggleLike("FORUM_POST", 999L, 100L, null));
    }

    @Test
    void toggleLike_videoNotFound() {
        when(videoRepository.findByIdAndStatus(999L, VideoStatus.NORMAL)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                likeService.toggleLike("VIDEO", 999L, 100L, null));
    }

    // ==================== Invalid targetType ====================

    @Test
    void toggleLike_invalidTargetType() {
        assertThrows(BusinessException.class, () ->
                likeService.toggleLike("INVALID", 1L, 100L, null));
    }

    // ==================== getLikeStatus ====================

    @Test
    void getLikeStatus_loggedInUser_liked() {
        when(likeRepository.existsByTargetTypeAndTargetIdAndUserId("TOOL", 1L, 100L))
                .thenReturn(true);
        when(toolRepository.findByIdAndStatusNormal(1L)).thenReturn(Optional.of(testTool));

        InteractionResponse response = likeService.getLikeStatus("TOOL", 1L, 100L, null);

        assertTrue(response.getLiked());
        assertEquals(5, response.getLikeCount());
    }

    @Test
    void getLikeStatus_anonymousUser_notLiked() {
        when(likeRepository.existsByTargetTypeAndTargetIdAndIpHash("TOOL", 1L, "somehash"))
                .thenReturn(false);
        when(toolRepository.findByIdAndStatusNormal(1L)).thenReturn(Optional.of(testTool));

        InteractionResponse response = likeService.getLikeStatus("TOOL", 1L, null, "somehash");

        assertFalse(response.getLiked());
        assertEquals(5, response.getLikeCount());
    }

    @Test
    void getLikeStatus_noUser_noIp() {
        when(toolRepository.findByIdAndStatusNormal(1L)).thenReturn(Optional.of(testTool));

        InteractionResponse response = likeService.getLikeStatus("TOOL", 1L, null, null);

        assertFalse(response.getLiked());
    }
}
