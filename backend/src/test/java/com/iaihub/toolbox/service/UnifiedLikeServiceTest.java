package com.iaihub.toolbox.service;

import com.iaihub.toolbox.dto.InteractionResponse;
import com.iaihub.toolbox.dto.PageResponse;
import com.iaihub.toolbox.dto.ToolSummaryDTO;
import com.iaihub.toolbox.exception.BusinessException;
import com.iaihub.toolbox.exception.ResourceNotFoundException;
import com.iaihub.toolbox.model.Tool;
import com.iaihub.toolbox.model.UnifiedLike;
import com.iaihub.toolbox.model.User;
import com.iaihub.toolbox.model.forum.ForumPost;
import com.iaihub.toolbox.model.forum.ForumPostStatus;
import com.iaihub.toolbox.model.video.Video;
import com.iaihub.toolbox.model.video.VideoStatus;
import com.iaihub.toolbox.repository.UnifiedLikeRepository;
import com.iaihub.toolbox.repository.PluginRepository;
import com.iaihub.toolbox.repository.ToolRepository;
import com.iaihub.toolbox.repository.UserRepository;
import com.iaihub.toolbox.repository.forum.ForumPostRepository;
import com.iaihub.toolbox.repository.video.VideoRepository;
import com.iaihub.toolbox.service.notification.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

    @Mock
    private PluginRepository pluginRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    private UnifiedLikeService likeService;

    private Tool testTool;
    private ForumPost testPost;
    private Video testVideo;
    private User testUser;

    @BeforeEach
    void setUp() {
        likeService = new UnifiedLikeService(likeRepository, toolRepository, forumPostRepository, videoRepository, pluginRepository, userRepository, notificationService);

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
                .authorId(100L)
                .likeCount(8)
                .status(ForumPostStatus.NORMAL)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testVideo = Video.builder()
                .id(20L)
                .title("Test Video")
                .uploaderId(100L)
                .likeCount(3)
                .status(VideoStatus.NORMAL)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testUser = User.builder()
                .id(100L).username("testuser").nickname("Test User")
                .avatarUrl("https://example.com/avatar.jpg")
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
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

    // ==================== getMyLikes ====================

    @Test
    void getMyLikes_loggedInUser_tool_returnsResource() {
        UnifiedLike like = UnifiedLike.builder()
                .id(1L).targetType("TOOL").targetId(1L).userId(100L)
                .createdAt(LocalDateTime.now()).build();
        Page<UnifiedLike> page = new PageImpl<>(List.of(like));
        when(likeRepository.findByUserIdAndTargetTypeOrderByCreatedAtDesc(eq(100L), eq("TOOL"), any(Pageable.class)))
                .thenReturn(page);
        when(toolRepository.findByIdAndStatusNormal(1L)).thenReturn(Optional.of(testTool));

        PageResponse<?> response = likeService.getMyLikes("TOOL", 100L, 0, 10);

        assertNotNull(response);
        assertEquals(1, response.getContent().size());
        assertTrue(response.getContent().get(0) instanceof ToolSummaryDTO);
        assertEquals("Test Tool", ((ToolSummaryDTO) response.getContent().get(0)).getName());
    }

    @Test
    void getMyLikes_forumPost_resolvesAuthorNickname() {
        UnifiedLike like = UnifiedLike.builder()
                .id(2L).targetType("FORUM_POST").targetId(10L).userId(100L)
                .createdAt(LocalDateTime.now()).build();
        Page<UnifiedLike> page = new PageImpl<>(List.of(like));
        when(likeRepository.findByUserIdAndTargetTypeOrderByCreatedAtDesc(eq(100L), eq("FORUM_POST"), any(Pageable.class)))
                .thenReturn(page);
        when(forumPostRepository.findById(10L)).thenReturn(Optional.of(testPost));
        when(userRepository.findById(100L)).thenReturn(Optional.of(testUser));

        PageResponse<?> response = likeService.getMyLikes("FORUM_POST", 100L, 0, 10);

        assertEquals(1, response.getContent().size());
        Object item = response.getContent().get(0);
        assertEquals(10L, ((UnifiedLikeService.ForumPostSummaryDTO) item).getId());
        assertEquals("Test Post", ((UnifiedLikeService.ForumPostSummaryDTO) item).getTitle());
        assertEquals("Test User", ((UnifiedLikeService.ForumPostSummaryDTO) item).getAuthorNickname());
    }

    @Test
    void getMyLikes_skipsDeletedTarget() {
        UnifiedLike like = UnifiedLike.builder()
                .id(3L).targetType("TOOL").targetId(999L).userId(100L)
                .createdAt(LocalDateTime.now()).build();
        Page<UnifiedLike> page = new PageImpl<>(List.of(like));
        when(likeRepository.findByUserIdAndTargetTypeOrderByCreatedAtDesc(eq(100L), eq("TOOL"), any(Pageable.class)))
                .thenReturn(page);
        when(toolRepository.findByIdAndStatusNormal(999L)).thenReturn(Optional.empty());

        PageResponse<?> response = likeService.getMyLikes("TOOL", 100L, 0, 10);

        assertEquals(0, response.getContent().size());
    }

    @Test
    void getMyLikes_anonymousUser_throws401() {
        assertThrows(BusinessException.class, () ->
                likeService.getMyLikes("TOOL", null, 0, 10));
    }

    // ==================== toggleLike: notification ====================

    @Test
    void toggleLike_notifiesTargetOwner_whenLikingOthersResource() {
        User owner = User.builder().id(200L).username("owner").nickname("Owner").build();
        Tool ownedTool = Tool.builder()
                .id(1L).name("Owned Tool").likeCount(5).status(Tool.Status.NORMAL)
                .uploader(owner)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
        when(toolRepository.findByIdAndStatusNormal(1L)).thenReturn(Optional.of(ownedTool));
        when(userRepository.findById(100L)).thenReturn(Optional.of(testUser));
        when(likeRepository.findByTargetTypeAndTargetIdAndUserId("TOOL", 1L, 100L))
                .thenReturn(Optional.empty());
        when(likeRepository.save(any(UnifiedLike.class))).thenAnswer(inv -> {
            UnifiedLike like = inv.getArgument(0);
            like.setId(1L);
            return like;
        });

        likeService.toggleLike("TOOL", 1L, 100L, null);

        verify(notificationService).createLikeNotification(
                eq(200L), eq("TOOL"), eq(1L), eq(100L), eq("Test User"));
    }

    @Test
    void toggleLike_doesNotNotify_whenUnliking() {
        UnifiedLike existing = UnifiedLike.builder().id(1L).targetType("TOOL").targetId(1L).userId(100L).build();
        when(toolRepository.findByIdAndStatusNormal(1L)).thenReturn(Optional.of(testTool));
        when(likeRepository.findByTargetTypeAndTargetIdAndUserId("TOOL", 1L, 100L))
                .thenReturn(Optional.of(existing));

        likeService.toggleLike("TOOL", 1L, 100L, null);

        verify(notificationService, never()).createLikeNotification(any(), any(), any(), any(), any());
    }
}
