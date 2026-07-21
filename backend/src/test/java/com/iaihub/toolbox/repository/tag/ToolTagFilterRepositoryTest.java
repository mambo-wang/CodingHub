package com.iaihub.toolbox.repository.tag;

import com.iaihub.toolbox.model.Category;
import com.iaihub.toolbox.model.Tool;
import com.iaihub.toolbox.model.User;
import com.iaihub.toolbox.model.tag.Tag;
import com.iaihub.toolbox.model.tag.TagType;
import com.iaihub.toolbox.model.tag.ToolTag;
import com.iaihub.toolbox.repository.CategoryRepository;
import com.iaihub.toolbox.repository.ToolRepository;
import com.iaihub.toolbox.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Task 1.3: ToolTagRepository.findToolIdsByTagId 和 ToolRepository.findByFiltersWithTag* 的集成测试
 */
@DataJpaTest
@ActiveProfiles("test")
class ToolTagFilterRepositoryTest {

    @Autowired
    private ToolTagRepository toolTagRepository;

    @Autowired
    private ToolRepository toolRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TagRepository tagRepository;

    private Category category;
    private User user;
    private Tag tagOpenSource;
    private Tag tagPython;
    private Tool tool1;
    private Tool tool2;
    private Tool tool3;

    @BeforeEach
    void setUp() {
        category = categoryRepository.save(Category.builder()
                .name("开发工具").sortOrder(0).build());
        user = userRepository.save(User.builder()
                .username("tester").nickname("测试员").password("pass123").build());

        tagOpenSource = tagRepository.save(Tag.builder()
                .name("开源").tagType(TagType.TOOL).usageCount(2).build());
        tagPython = tagRepository.save(Tag.builder()
                .name("Python").tagType(TagType.TOOL).usageCount(1).build());

        tool1 = toolRepository.save(Tool.builder()
                .name("Alpha工具").content("content alpha")
                .category(category).uploader(user)
                .version("1.0.0").build());
        tool2 = toolRepository.save(Tool.builder()
                .name("Beta工具").content("content beta")
                .category(category).uploader(user)
                .version("1.0.0").build());
        tool3 = toolRepository.save(Tool.builder()
                .name("Gamma工具").content("content gamma")
                .category(category).uploader(user)
                .version("1.0.0").build());

        // tool1 → 开源, Python; tool2 → 开源; tool3 → 无标签
        toolTagRepository.save(new ToolTag(tool1.getId(), tagOpenSource.getId()));
        toolTagRepository.save(new ToolTag(tool1.getId(), tagPython.getId()));
        toolTagRepository.save(new ToolTag(tool2.getId(), tagOpenSource.getId()));
    }

    // ── ToolTagRepository.findToolIdsByTagId ──

    @Test
    void findToolIdsByTagId_returnsAssociatedToolIds() {
        List<Long> ids = toolTagRepository.findToolIdsByTagId(tagOpenSource.getId());
        assertEquals(2, ids.size());
        assertTrue(ids.contains(tool1.getId()));
        assertTrue(ids.contains(tool2.getId()));
    }

    @Test
    void findToolIdsByTagId_noAssociation_returnsEmpty() {
        List<Long> ids = toolTagRepository.findToolIdsByTagId(9999L);
        assertTrue(ids.isEmpty());
    }

    // ── ToolRepository.findByFiltersWithTag ──

    @Test
    void findByFiltersWithTag_returnsOnlyTaggedTools() {
        Page<Tool> page = toolRepository.findByFiltersWithTag(
                null, null, tagOpenSource.getId(), PageRequest.of(0, 10));
        assertEquals(2, page.getTotalElements());
        assertTrue(page.getContent().stream()
                .allMatch(t -> t.getId().equals(tool1.getId()) || t.getId().equals(tool2.getId())));
    }

    @Test
    void findByFiltersWithTag_withKeyword_filtersBoth() {
        Page<Tool> page = toolRepository.findByFiltersWithTag(
                null, "Alpha", tagOpenSource.getId(), PageRequest.of(0, 10));
        assertEquals(1, page.getTotalElements());
        assertEquals("Alpha工具", page.getContent().get(0).getName());
    }

    @Test
    void findByFiltersWithTag_withCategory_filtersBoth() {
        Page<Tool> page = toolRepository.findByFiltersWithTag(
                category.getId(), null, tagPython.getId(), PageRequest.of(0, 10));
        assertEquals(1, page.getTotalElements());
        assertEquals(tool1.getId(), page.getContent().get(0).getId());
    }

    @Test
    void findByFiltersWithTag_noMatchingTag_returnsEmpty() {
        Page<Tool> page = toolRepository.findByFiltersWithTag(
                null, null, 9999L, PageRequest.of(0, 10));
        assertEquals(0, page.getTotalElements());
    }

    @Test
    void findByFiltersWithTagOrderByName_sortsByNameAsc() {
        Page<Tool> page = toolRepository.findByFiltersWithTagOrderByName(
                null, null, tagOpenSource.getId(), PageRequest.of(0, 10));
        assertEquals(2, page.getTotalElements());
        assertEquals("Alpha工具", page.getContent().get(0).getName());
        assertEquals("Beta工具", page.getContent().get(1).getName());
    }

    @Test
    void findByFiltersWithTagOrderByHot_returnsTaggedTools() {
        Page<Tool> page = toolRepository.findByFiltersWithTagOrderByHot(
                null, null, tagOpenSource.getId(), PageRequest.of(0, 10));
        assertEquals(2, page.getTotalElements());
    }
}
