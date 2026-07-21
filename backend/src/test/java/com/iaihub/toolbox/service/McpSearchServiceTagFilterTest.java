package com.iaihub.toolbox.service;

import com.iaihub.toolbox.dto.ToolSearchResult;
import com.iaihub.toolbox.dto.tag.TagDTO;
import com.iaihub.toolbox.model.Category;
import com.iaihub.toolbox.model.Tool;
import com.iaihub.toolbox.model.tag.Tag;
import com.iaihub.toolbox.model.tag.TagType;
import com.iaihub.toolbox.model.tag.ToolTag;
import com.iaihub.toolbox.repository.ToolRepository;
import com.iaihub.toolbox.repository.ToolFileRepository;
import com.iaihub.toolbox.repository.UserRepository;
import com.iaihub.toolbox.repository.forum.ForumPostRepository;
import com.iaihub.toolbox.repository.tag.TagRepository;
import com.iaihub.toolbox.repository.tag.ToolTagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * Task 7.5: McpSearchService.searchTools 标签过滤单元测试
 * 覆盖：tag 为空（向后兼容）、tag 有值过滤、tag+query 叠加、大小写不敏感、tag 无匹配返回空
 */
@ExtendWith(MockitoExtension.class)
class McpSearchServiceTagFilterTest {

    @Mock
    private ToolRepository toolRepository;
    @Mock
    private ToolFileRepository toolFileRepository;
    @Mock
    private ForumPostRepository forumPostRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ToolTagRepository toolTagRepository;
    @Mock
    private TagRepository tagRepository;

    private McpSearchService mcpSearchService;

    private Tool tool1;
    private Tool tool2;
    private Tag tagOpenSource;
    private Tag tagPython;

    @BeforeEach
    void setUp() {
        mcpSearchService = new McpSearchService(toolRepository, toolFileRepository,
                forumPostRepository, userRepository, toolTagRepository, tagRepository);

        Category cat = Category.builder().id(1L).name("开发工具").build();
        tool1 = Tool.builder()
                .id(1L).name("Alpha工具").content("content alpha")
                .category(cat).version("1.0.0")
                .createdAt(LocalDateTime.now()).build();
        tool2 = Tool.builder()
                .id(2L).name("Beta工具").content("content beta")
                .category(cat).version("2.0.0")
                .createdAt(LocalDateTime.now()).build();

        tagOpenSource = Tag.builder().id(10L).name("开源").tagType(TagType.TOOL).usageCount(2).build();
        tagPython = Tag.builder().id(11L).name("Python").tagType(TagType.TOOL).usageCount(1).build();
    }

    private void stubToolTags(Long toolId, Tag... tags) {
        List<ToolTag> tts = java.util.Arrays.stream(tags)
                .map(t -> new ToolTag(toolId, t.getId()))
                .toList();
        when(toolTagRepository.findByToolId(toolId)).thenReturn(tts);
    }

    @Test
    void searchTools_tagNull_backwardCompatible_returnsAll() {
        // Given: tag 为空，返回所有工具
        when(toolRepository.findApprovedToolsWithCategory(isNull(), any(Pageable.class)))
                .thenReturn(List.of(tool1, tool2));
        stubToolTags(1L, tagOpenSource);
        stubToolTags(2L);

        // When
        List<ToolSearchResult> results = mcpSearchService.searchTools(null, null, null, 20);

        // Then: 不过滤，返回全部
        assertEquals(2, results.size());
    }

    @Test
    void searchTools_tagPresent_filtersToTaggedTools() {
        // Given: tag="开源"，只有 tool1 关联了"开源"标签
        when(toolRepository.findApprovedToolsWithCategory(isNull(), any(Pageable.class)))
                .thenReturn(List.of(tool1, tool2));
        stubToolTags(1L, tagOpenSource, tagPython);
        stubToolTags(2L, tagPython);
        when(tagRepository.findAllById(anyCollection()))
                .thenReturn(List.of(tagOpenSource, tagPython));

        // When
        List<ToolSearchResult> results = mcpSearchService.searchTools(null, null, "开源", 20);

        // Then: 只返回 tool1
        assertEquals(1, results.size());
        assertEquals(1L, results.get(0).getId());
    }

    @Test
    void searchTools_tagPlusQuery_bothFiltersApplied() {
        // Given: query="Alpha", tag="开源"
        when(toolRepository.findApprovedToolsWithCategory(eq("Alpha"), any(Pageable.class)))
                .thenReturn(List.of(tool1));
        stubToolTags(1L, tagOpenSource);
        when(tagRepository.findAllById(anyCollection()))
                .thenReturn(List.of(tagOpenSource));

        // When
        List<ToolSearchResult> results = mcpSearchService.searchTools("Alpha", null, "开源", 20);

        // Then
        assertEquals(1, results.size());
        assertEquals("Alpha工具", results.get(0).getName());
    }

    @Test
    void searchTools_tagCaseInsensitive_matches() {
        // Given: tag="PYTHON"（大写），应匹配 name="Python" 的标签
        when(toolRepository.findApprovedToolsWithCategory(isNull(), any(Pageable.class)))
                .thenReturn(List.of(tool1, tool2));
        stubToolTags(1L, tagOpenSource);
        stubToolTags(2L, tagPython);
        when(tagRepository.findAllById(anyCollection()))
                .thenReturn(List.of(tagOpenSource, tagPython));

        // When
        List<ToolSearchResult> results = mcpSearchService.searchTools(null, null, "PYTHON", 20);

        // Then: 只有 tool2 关联了 Python 标签
        assertEquals(1, results.size());
        assertEquals(2L, results.get(0).getId());
    }

    @Test
    void searchTools_tagNoMatch_returnsEmpty() {
        // Given: tag="不存在的标签"
        when(toolRepository.findApprovedToolsWithCategory(isNull(), any(Pageable.class)))
                .thenReturn(List.of(tool1, tool2));
        stubToolTags(1L, tagOpenSource);
        stubToolTags(2L, tagPython);
        when(tagRepository.findAllById(anyCollection()))
                .thenReturn(List.of(tagOpenSource, tagPython));

        // When
        List<ToolSearchResult> results = mcpSearchService.searchTools(null, null, "不存在的标签", 20);

        // Then
        assertTrue(results.isEmpty());
    }
}
