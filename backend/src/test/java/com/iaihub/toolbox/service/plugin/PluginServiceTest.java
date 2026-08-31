package com.iaihub.toolbox.service.plugin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iaihub.toolbox.config.UploadConfig;
import com.iaihub.toolbox.dto.plugin.PluginSummaryDTO;
import com.iaihub.toolbox.exception.ResourceNotFoundException;
import com.iaihub.toolbox.model.Plugin;
import com.iaihub.toolbox.model.tag.PluginTag;
import com.iaihub.toolbox.model.tag.Tag;
import com.iaihub.toolbox.model.tag.TagType;
import com.iaihub.toolbox.repository.PluginRepository;
import com.iaihub.toolbox.repository.UserRepository;
import com.iaihub.toolbox.repository.tag.PluginTagRepository;
import com.iaihub.toolbox.repository.tag.TagRepository;
import com.iaihub.toolbox.service.tag.TagService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PluginServiceTest {

    @Mock
    private PluginRepository pluginRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UploadConfig uploadConfig;
    @Mock
    private PluginTagRepository pluginTagRepository;
    @Mock
    private TagRepository tagRepository;
    @Mock
    private TagService tagService;

    private PluginService pluginService;

    @BeforeEach
    void setUp() {
        pluginService = new PluginService(
                pluginRepository, userRepository, uploadConfig, new ObjectMapper(),
                pluginTagRepository, tagRepository, tagService);
    }

    private Plugin samplePlugin(Long id) {
        return Plugin.builder()
                .id(id).name("demo-plugin").version("1.0.0")
                .status(Plugin.Status.NORMAL)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
    }

    // ==================== 置顶 ====================

    @Test
    void pinPlugin_callsPinById() {
        when(pluginRepository.findById(1L)).thenReturn(Optional.of(samplePlugin(1L)));
        pluginService.pinPlugin(1L);
        verify(pluginRepository).pinById(1L);
    }

    @Test
    void unpinPlugin_callsUnpinById() {
        when(pluginRepository.findById(1L)).thenReturn(Optional.of(samplePlugin(1L)));
        pluginService.unpinPlugin(1L);
        verify(pluginRepository).unpinById(1L);
    }

    @Test
    void pinPlugin_missing_throwsNotFound() {
        when(pluginRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> pluginService.pinPlugin(99L));
        verify(pluginRepository, never()).pinById(any());
    }

    @Test
    void unpinPlugin_missing_throwsNotFound() {
        when(pluginRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> pluginService.unpinPlugin(99L));
        verify(pluginRepository, never()).unpinById(any());
    }

    // ==================== 热度 Top5 ====================

    @Test
    void getHotTop5_returnsIds() {
        when(pluginRepository.findTop5ByStatusOrderByScoreDesc(any(Pageable.class)))
                .thenReturn(List.of(3L, 1L, 2L));
        List<Long> top = pluginService.getHotTop5();
        assertEquals(List.of(3L, 1L, 2L), top);
    }

    // ==================== pinned 字段 ====================

    @Test
    void pluginPinnedDefaultsFalse() {
        Plugin p = samplePlugin(1L);
        assertFalse(Boolean.TRUE.equals(p.getPinned()));
    }

    // ==================== 标签 ====================

    @Test
    void replaceTags_rebuildsAssociationsAndUsageCounts() {
        when(pluginTagRepository.findByPluginId(5L)).thenReturn(List.of(new PluginTag(5L, 20L)));
        Tag oldTag = Tag.builder().id(20L).name("old").tagType(TagType.PLUGIN).usageCount(1).build();
        Tag newTag = Tag.builder().id(21L).name("new").tagType(TagType.PLUGIN).usageCount(0).build();
        when(tagRepository.findById(20L)).thenReturn(Optional.of(oldTag));
        when(tagRepository.findById(21L)).thenReturn(Optional.of(newTag));

        pluginService.replaceTags(5L, List.of(21L));

        verify(pluginTagRepository).deleteByPluginId(5L);
        verify(pluginTagRepository).save(new PluginTag(5L, 21L));
        assertEquals(0, oldTag.getUsageCount());
        assertEquals(1, newTag.getUsageCount());
    }

    @Test
    void replaceTags_nullClearsAll() {
        when(pluginTagRepository.findByPluginId(5L)).thenReturn(List.of());

        pluginService.replaceTags(5L, null);

        verify(pluginTagRepository).deleteByPluginId(5L);
        verify(pluginTagRepository, never()).save(any(PluginTag.class));
    }

    @Test
    void listSummaryCarriesTagsAndPinned() {
        Plugin p = samplePlugin(1L);
        p.setPinned(true);
        p.setFavoriteCount(2);
        when(pluginRepository.findByFilters(eq(null), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(p)));
        when(pluginTagRepository.findByPluginId(1L)).thenReturn(List.of(new PluginTag(1L, 42L)));
        Tag tag = Tag.builder().id(42L).name("效率").tagType(TagType.PLUGIN).usageCount(1).build();
        when(tagRepository.findById(42L)).thenReturn(Optional.of(tag));
        when(tagService.toDTO(tag)).thenReturn(
                new com.iaihub.toolbox.dto.tag.TagDTO(42L, "效率", "PLUGIN", 1));

        var result = pluginService.list(null, null, 0, 12, "new");

        PluginSummaryDTO dto = (PluginSummaryDTO) result.getContent().get(0);
        assertTrue(dto.getPinned());
        assertEquals(2, dto.getFavoriteCount());
        assertEquals(1, dto.getTags().size());
        assertEquals("效率", dto.getTags().get(0).name());
    }
}
