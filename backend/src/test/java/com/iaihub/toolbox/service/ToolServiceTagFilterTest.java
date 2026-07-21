package com.iaihub.toolbox.service;

import com.iaihub.toolbox.dto.PageResponse;
import com.iaihub.toolbox.dto.ToolSummaryDTO;
import com.iaihub.toolbox.model.Category;
import com.iaihub.toolbox.model.Tool;
import com.iaihub.toolbox.model.User;
import com.iaihub.toolbox.repository.CategoryRepository;
import com.iaihub.toolbox.repository.ToolRepository;
import com.iaihub.toolbox.repository.UserRepository;
import com.iaihub.toolbox.repository.tag.TagRepository;
import com.iaihub.toolbox.repository.tag.ToolTagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Task 2.2: ToolService.getTools 标签筛选单元测试
 * 覆盖：tagId 为空（向后兼容）、tagId 有值、tagId+categoryId+keyword 叠加、tagId 无关联工具返回空
 */
@ExtendWith(MockitoExtension.class)
class ToolServiceTagFilterTest {

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

    private ToolService toolService;

    private Tool sampleTool;

    @BeforeEach
    void setUp() {
        toolService = new ToolService(toolRepository, categoryRepository,
                userRepository, toolFileService, tagRepository, toolTagRepository);

        Category cat = Category.builder().id(1L).name("开发工具").build();
        User user = User.builder().id(1L).username("tester").nickname("测试员").build();
        sampleTool = Tool.builder()
                .id(10L).name("测试工具").content("内容")
                .category(cat).uploader(user)
                .version("1.0.0")
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .viewCount(0).likeCount(0).commentCount(0)
                .build();
    }

    @Test
    void getTools_tagIdNull_backwardCompatible_callsNonTagQuery() {
        // Given: tagId 为空，sortBy 默认 hot
        Page<Tool> page = new PageImpl<>(List.of(sampleTool));
        when(toolRepository.findByFiltersOrderByHot(isNull(), isNull(), any(Pageable.class)))
                .thenReturn(page);

        // When
        PageResponse<ToolSummaryDTO> result = toolService.getTools(null, null, null, "hot", 0, 12);

        // Then: 调用不带 tag 的旧方法
        verify(toolRepository).findByFiltersOrderByHot(isNull(), isNull(), any(Pageable.class));
        verify(toolRepository, never()).findByFiltersWithTag(any(), any(), any(), any());
        assertEquals(1, result.getContent().size());
    }

    @Test
    void getTools_tagIdPresent_callsTagQuery() {
        // Given: tagId=5, sortBy=hot
        Page<Tool> page = new PageImpl<>(List.of(sampleTool));
        when(toolRepository.findByFiltersWithTagOrderByHot(isNull(), isNull(), eq(5L), any(Pageable.class)))
                .thenReturn(page);

        // When
        PageResponse<ToolSummaryDTO> result = toolService.getTools(null, null, 5L, "hot", 0, 12);

        // Then
        verify(toolRepository).findByFiltersWithTagOrderByHot(isNull(), isNull(), eq(5L), any(Pageable.class));
        assertEquals(1, result.getContent().size());
    }

    @Test
    void getTools_tagIdWithSortByName_callsNameQuery() {
        Page<Tool> page = new PageImpl<>(List.of(sampleTool));
        when(toolRepository.findByFiltersWithTagOrderByName(isNull(), isNull(), eq(5L), any(Pageable.class)))
                .thenReturn(page);

        toolService.getTools(null, null, 5L, "name", 0, 12);

        verify(toolRepository).findByFiltersWithTagOrderByName(isNull(), isNull(), eq(5L), any(Pageable.class));
    }

    @Test
    void getTools_tagIdWithSortByLatest_callsLatestQuery() {
        Page<Tool> page = new PageImpl<>(List.of(sampleTool));
        when(toolRepository.findByFiltersWithTag(isNull(), isNull(), eq(5L), any(Pageable.class)))
                .thenReturn(page);

        toolService.getTools(null, null, 5L, "latest", 0, 12);

        verify(toolRepository).findByFiltersWithTag(isNull(), isNull(), eq(5L), any(Pageable.class));
    }

    @Test
    void getTools_tagIdPlusCategoryPlusKeyword_allParamsPassed() {
        // Given: tagId=5, categoryId=1, keyword="测试"
        Page<Tool> page = new PageImpl<>(List.of(sampleTool));
        when(toolRepository.findByFiltersWithTagOrderByHot(eq(1L), eq("测试"), eq(5L), any(Pageable.class)))
                .thenReturn(page);

        // When
        PageResponse<ToolSummaryDTO> result = toolService.getTools(1L, "测试", 5L, "hot", 0, 12);

        // Then: 三个条件叠加传递
        verify(toolRepository).findByFiltersWithTagOrderByHot(eq(1L), eq("测试"), eq(5L), any(Pageable.class));
        assertEquals(1, result.getContent().size());
    }

    @Test
    void getTools_tagIdNoMatch_returnsEmpty() {
        // Given: tagId 存在但无关联工具
        Page<Tool> emptyPage = new PageImpl<>(Collections.emptyList());
        when(toolRepository.findByFiltersWithTagOrderByHot(isNull(), isNull(), eq(999L), any(Pageable.class)))
                .thenReturn(emptyPage);

        // When
        PageResponse<ToolSummaryDTO> result = toolService.getTools(null, null, 999L, "hot", 0, 12);

        // Then
        assertTrue(result.getContent().isEmpty());
        assertEquals(0, result.getTotalElements());
    }
}
