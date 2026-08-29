package com.iaihub.toolbox.service;

import com.iaihub.toolbox.dto.InteractionResponse;
import com.iaihub.toolbox.dto.PageResponse;
import com.iaihub.toolbox.exception.BusinessException;
import com.iaihub.toolbox.exception.ResourceNotFoundException;
import com.iaihub.toolbox.model.*;
import com.iaihub.toolbox.model.video.Video;
import com.iaihub.toolbox.model.video.VideoStatus;
import com.iaihub.toolbox.repository.UnifiedFavoriteRepository;
import com.iaihub.toolbox.repository.PluginRepository;
import com.iaihub.toolbox.repository.ToolRepository;
import com.iaihub.toolbox.repository.UserRepository;
import com.iaihub.toolbox.repository.forum.ForumPostRepository;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UnifiedFavoriteServiceTest {

    @Mock
    private UnifiedFavoriteRepository favoriteRepository;

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

    private UnifiedFavoriteService favoriteService;

    private Tool testTool;
    private User testUser;

    @BeforeEach
    void setUp() {
        favoriteService = new UnifiedFavoriteService(
                favoriteRepository, toolRepository, forumPostRepository, videoRepository, pluginRepository, userRepository);

        testUser = User.builder()
                .id(100L).username("testuser").nickname("Test User")
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();

        Category category = Category.builder()
                .id(1L).name("Skill").icon("Wrench").build();

        testTool = Tool.builder()
                .id(1L).name("Test Tool").version("1.0.0")
                .category(category)
                .uploader(testUser)
                .status(Tool.Status.NORMAL)
                .likeCount(0).commentCount(0).viewCount(0)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
    }

    // ==================== toggleFavorite ====================

    @Test
    void toggleFavorite_addFavorite() {
        when(toolRepository.findByIdAndStatusNormal(1L)).thenReturn(Optional.of(testTool));
        when(favoriteRepository.findByUserIdAndTargetTypeAndTargetId(100L, "TOOL", 1L))
                .thenReturn(Optional.empty());
        when(favoriteRepository.save(any(UnifiedFavorite.class))).thenAnswer(inv -> inv.getArgument(0));

        InteractionResponse response = favoriteService.toggleFavorite("TOOL", 1L, 100L);

        assertTrue(response.getFavorited());
        verify(favoriteRepository).save(any(UnifiedFavorite.class));
    }

    @Test
    void toggleFavorite_removeFavorite() {
        UnifiedFavorite existing = UnifiedFavorite.builder()
                .id(1L).targetType("TOOL").targetId(1L).userId(100L).build();

        when(toolRepository.findByIdAndStatusNormal(1L)).thenReturn(Optional.of(testTool));
        when(favoriteRepository.findByUserIdAndTargetTypeAndTargetId(100L, "TOOL", 1L))
                .thenReturn(Optional.of(existing));

        InteractionResponse response = favoriteService.toggleFavorite("TOOL", 1L, 100L);

        assertFalse(response.getFavorited());
        verify(favoriteRepository).deleteByUserIdAndTargetTypeAndTargetId(100L, "TOOL", 1L);
    }

    @Test
    void toggleFavorite_plugin_add_increments_favorite_count() {
        Plugin testPlugin = Plugin.builder()
                .id(10L).name("demo-plugin").version("1.0.0")
                .author(testUser).status(Plugin.Status.NORMAL)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
        when(pluginRepository.findByIdAndStatusNormal(10L)).thenReturn(Optional.of(testPlugin));
        when(favoriteRepository.findByUserIdAndTargetTypeAndTargetId(100L, "PLUGIN", 10L))
                .thenReturn(Optional.empty());
        when(favoriteRepository.save(any(UnifiedFavorite.class))).thenAnswer(inv -> inv.getArgument(0));

        InteractionResponse response = favoriteService.toggleFavorite("PLUGIN", 10L, 100L);

        assertTrue(response.getFavorited());
        verify(pluginRepository).incrementFavoriteCount(10L);
        verify(pluginRepository, never()).decrementFavoriteCount(anyLong());
    }

    @Test
    void toggleFavorite_plugin_remove_decrements_favorite_count() {
        Plugin testPlugin = Plugin.builder()
                .id(10L).name("demo-plugin").version("1.0.0")
                .author(testUser).status(Plugin.Status.NORMAL)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
        UnifiedFavorite existing = UnifiedFavorite.builder()
                .id(1L).targetType("PLUGIN").targetId(10L).userId(100L).build();
        when(pluginRepository.findByIdAndStatusNormal(10L)).thenReturn(Optional.of(testPlugin));
        when(favoriteRepository.findByUserIdAndTargetTypeAndTargetId(100L, "PLUGIN", 10L))
                .thenReturn(Optional.of(existing));

        InteractionResponse response = favoriteService.toggleFavorite("PLUGIN", 10L, 100L);

        assertFalse(response.getFavorited());
        verify(pluginRepository).decrementFavoriteCount(10L);
        verify(pluginRepository, never()).incrementFavoriteCount(anyLong());
    }

    @Test
    void pluginUpdateScore_includes_favorite_weight_four() {
        Plugin p = Plugin.builder()
                .viewCount(1).likeCount(1).commentCount(1).favoriteCount(3)
                .build();
        p.updateScore();
        // score = 1×1 + 1×3 + 3×4 + 1×5 = 21
        assertEquals(0, new java.math.BigDecimal("21").compareTo(p.getScore()));
    }

    @Test
    void toggleFavorite_notLoggedIn() {
        assertThrows(BusinessException.class, () ->
                favoriteService.toggleFavorite("TOOL", 1L, null));
    }

    @Test
    void toggleFavorite_resourceNotFound() {
        when(toolRepository.findByIdAndStatusNormal(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                favoriteService.toggleFavorite("TOOL", 999L, 100L));
    }

    @Test
    void toggleFavorite_deletedResource() {
        when(toolRepository.findByIdAndStatusNormal(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                favoriteService.toggleFavorite("TOOL", 1L, 100L));
    }

    // ==================== getFavoriteStatus ====================

    @Test
    void getFavoriteStatus_favorited() {
        when(favoriteRepository.existsByUserIdAndTargetTypeAndTargetId(100L, "TOOL", 1L))
                .thenReturn(true);

        InteractionResponse response = favoriteService.getFavoriteStatus("TOOL", 1L, 100L);

        assertTrue(response.getFavorited());
    }

    @Test
    void getFavoriteStatus_notFavorited() {
        when(favoriteRepository.existsByUserIdAndTargetTypeAndTargetId(100L, "TOOL", 1L))
                .thenReturn(false);

        InteractionResponse response = favoriteService.getFavoriteStatus("TOOL", 1L, 100L);

        assertFalse(response.getFavorited());
    }

    @Test
    void getFavoriteStatus_notLoggedIn() {
        assertThrows(BusinessException.class, () ->
                favoriteService.getFavoriteStatus("TOOL", 1L, null));
    }

    // ==================== getMyFavorites ====================

    @Test
    void getMyFavorites_toolFavorites() {
        UnifiedFavorite fav = UnifiedFavorite.builder()
                .id(1L).targetType("TOOL").targetId(1L).userId(100L)
                .createdAt(LocalDateTime.now()).build();

        Page<UnifiedFavorite> page = new PageImpl<>(List.of(fav));
        when(favoriteRepository.findByUserIdAndTargetTypeOrderByCreatedAtDesc(
                eq(100L), eq("TOOL"), any(Pageable.class))).thenReturn(page);
        when(toolRepository.findByIdAndStatusNormal(1L)).thenReturn(Optional.of(testTool));

        PageResponse<?> response = favoriteService.getMyFavorites("TOOL", 100L, 0, 10);

        assertNotNull(response);
        assertEquals(1, response.getContent().size());
    }

    @Test
    void getMyFavorites_emptyList() {
        Page<UnifiedFavorite> emptyPage = new PageImpl<>(List.of());
        when(favoriteRepository.findByUserIdAndTargetTypeOrderByCreatedAtDesc(
                eq(100L), eq("TOOL"), any(Pageable.class))).thenReturn(emptyPage);

        PageResponse<?> response = favoriteService.getMyFavorites("TOOL", 100L, 0, 10);

        assertNotNull(response);
        assertTrue(response.getContent().isEmpty());
    }

    @Test
    void getMyFavorites_notLoggedIn() {
        assertThrows(BusinessException.class, () ->
                favoriteService.getMyFavorites("TOOL", null, 0, 10));
    }

    @Test
    void getMyFavorites_videoFavorites() {
        Video video = Video.builder()
                .id(20L).title("Test Video")
                .uploaderId(100L).status(VideoStatus.NORMAL)
                .viewCount(0).likeCount(0).commentCount(0)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();

        UnifiedFavorite fav = UnifiedFavorite.builder()
                .id(1L).targetType("VIDEO").targetId(20L).userId(100L)
                .createdAt(LocalDateTime.now()).build();

        Page<UnifiedFavorite> page = new PageImpl<>(List.of(fav));
        when(favoriteRepository.findByUserIdAndTargetTypeOrderByCreatedAtDesc(
                eq(100L), eq("VIDEO"), any(Pageable.class))).thenReturn(page);
        when(videoRepository.findByIdAndStatus(20L, VideoStatus.NORMAL)).thenReturn(Optional.of(video));
        when(userRepository.findById(100L)).thenReturn(Optional.of(testUser));

        PageResponse<?> response = favoriteService.getMyFavorites("VIDEO", 100L, 0, 10);

        assertNotNull(response);
        assertEquals(1, response.getContent().size());
    }

    // ==================== Invalid targetType ====================

    @Test
    void toggleFavorite_invalidTargetType() {
        assertThrows(BusinessException.class, () ->
                favoriteService.toggleFavorite("INVALID", 1L, 100L));
    }
}
