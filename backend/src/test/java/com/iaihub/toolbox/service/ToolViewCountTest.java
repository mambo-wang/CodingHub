package com.iaihub.toolbox.service;

import com.iaihub.toolbox.dto.ToolDetailDTO;
import com.iaihub.toolbox.model.Category;
import com.iaihub.toolbox.model.Role;
import com.iaihub.toolbox.model.Tool;
import com.iaihub.toolbox.model.User;
import com.iaihub.toolbox.repository.CategoryRepository;
import com.iaihub.toolbox.repository.ToolFileRepository;
import com.iaihub.toolbox.repository.ToolRepository;
import com.iaihub.toolbox.repository.UnifiedFavoriteRepository;
import com.iaihub.toolbox.repository.UserRepository;
import com.iaihub.toolbox.repository.tag.TagRepository;
import com.iaihub.toolbox.repository.tag.ToolTagRepository;
import com.iaihub.toolbox.service.ToolFileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ToolViewCountTest {

    @Mock
    private ToolRepository toolRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ToolFileService toolFileService;
    @Mock
    private TagRepository tagRepository;
    @Mock
    private ToolTagRepository toolTagRepository;
    @Mock
    private UnifiedFavoriteRepository unifiedFavoriteRepository;
    @Mock
    private ToolFileRepository toolFileRepository;

    private ToolService toolService;

    @BeforeEach
    void setUp() {
        toolService = new ToolService(
                toolRepository,
                categoryRepository,
                userRepository,
                toolFileService,
                tagRepository,
                toolTagRepository,
                unifiedFavoriteRepository,
                toolFileRepository);
    }

    @Test
    void getToolById_shouldIncrementViewCount() {
        // Given: 当前浏览量为 5
        Tool tool = Tool.builder()
                .id(1L)
                .name("测试工具")
                .content("描述")
                .version("3.0.0")
                .category(Category.builder().id(1L).name("计算机视觉").icon("👁️").build())
                .uploader(User.builder().id(1L).username("u").nickname("U").role(Role.USER).build())
                .status(Tool.Status.NORMAL)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .viewCount(5)
                .likeCount(0)
                .commentCount(0)
                .score(BigDecimal.ZERO)
                .build();

        when(toolRepository.findByIdAndStatusNormal(1L)).thenReturn(Optional.of(tool));
        when(toolRepository.save(any(Tool.class))).thenReturn(tool);
        when(toolTagRepository.findByToolId(1L)).thenReturn(Collections.emptyList());
        when(unifiedFavoriteRepository.countByTargetTypeAndTargetId(eq("TOOL"), eq(1L))).thenReturn(0L);
        when(toolFileRepository.sumDownloadCountGroupByToolId(any())).thenReturn(Collections.emptyList());

        // When
        ToolDetailDTO result = toolService.getToolById(1L);

        // Then: 浏览量自增为 6，并持久化
        assertEquals(6, result.getViewCount());
        verify(toolRepository).save(tool);
    }
}
