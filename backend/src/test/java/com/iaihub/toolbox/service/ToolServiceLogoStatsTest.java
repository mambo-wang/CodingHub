package com.iaihub.toolbox.service;

import com.iaihub.toolbox.dto.PageResponse;
import com.iaihub.toolbox.dto.ToolSummaryDTO;
import com.iaihub.toolbox.model.Category;
import com.iaihub.toolbox.model.Tool;
import com.iaihub.toolbox.model.User;
import com.iaihub.toolbox.repository.ToolFileRepository;
import com.iaihub.toolbox.repository.ToolRepository;
import com.iaihub.toolbox.repository.UnifiedFavoriteRepository;
import com.iaihub.toolbox.repository.UserRepository;
import com.iaihub.toolbox.repository.CategoryRepository;
import com.iaihub.toolbox.repository.tag.TagRepository;
import com.iaihub.toolbox.repository.tag.ToolTagRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ToolServiceLogoStatsTest {

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

    private ToolService newService() {
        return new ToolService(toolRepository, categoryRepository, userRepository, toolFileService,
                tagRepository, toolTagRepository, unifiedFavoriteRepository, toolFileRepository);
    }

    private User uploader() {
        return User.builder().id(1L).username("wangbao").nickname("王宝").build();
    }

    private Tool tool(Long id, String logoUrl, Category category) {
        return Tool.builder()
                .id(id)
                .name("tool-" + id)
                .category(category)
                .content("c")
                .uploader(uploader())
                .logoUrl(logoUrl)
                .viewCount(5)
                .likeCount(2)
                .commentCount(1)
                .build();
    }

    @Test
    void logoFallback_toolLogoWins() {
        Category cat = Category.builder().id(1L).name("Skill").icon("🔧").logoUrl("/cat.png").build();
        Tool t = tool(1L, "/tool.png", cat);
        when(toolRepository.findByFiltersOrderByHot(isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(t)));
        when(unifiedFavoriteRepository.countByTargetTypeAndTargetIdIn(eq("TOOL"), anyCollection()))
                .thenReturn(Collections.emptyList());
        when(toolFileRepository.sumDownloadCountGroupByToolId(anyCollection()))
                .thenReturn(Collections.emptyList());
        when(toolTagRepository.findByToolId(anyLong())).thenReturn(Collections.emptyList());

        PageResponse<ToolSummaryDTO> resp = newService().getTools(null, null, null, "hot", 0, 10);

        assertEquals("/tool.png", resp.getContent().get(0).getLogoUrl());
    }

    @Test
    void logoFallback_categoryLogoWhenToolLogoMissing() {
        Category cat = Category.builder().id(1L).name("Skill").icon("🔧").logoUrl("/cat.png").build();
        Tool t = tool(2L, null, cat);
        when(toolRepository.findByFiltersOrderByHot(isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(t)));
        when(unifiedFavoriteRepository.countByTargetTypeAndTargetIdIn(eq("TOOL"), anyCollection()))
                .thenReturn(Collections.emptyList());
        when(toolFileRepository.sumDownloadCountGroupByToolId(anyCollection()))
                .thenReturn(Collections.emptyList());
        when(toolTagRepository.findByToolId(anyLong())).thenReturn(Collections.emptyList());

        PageResponse<ToolSummaryDTO> resp = newService().getTools(null, null, null, "hot", 0, 10);

        assertEquals("/cat.png", resp.getContent().get(0).getLogoUrl());
    }

    @Test
    void logoFallback_nullWhenBothMissing() {
        Category cat = Category.builder().id(1L).name("Skill").icon("🔧").logoUrl(null).build();
        Tool t = tool(3L, null, cat);
        when(toolRepository.findByFiltersOrderByHot(isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(t)));
        when(unifiedFavoriteRepository.countByTargetTypeAndTargetIdIn(eq("TOOL"), anyCollection()))
                .thenReturn(Collections.emptyList());
        when(toolFileRepository.sumDownloadCountGroupByToolId(anyCollection()))
                .thenReturn(Collections.emptyList());
        when(toolTagRepository.findByToolId(anyLong())).thenReturn(Collections.emptyList());

        PageResponse<ToolSummaryDTO> resp = newService().getTools(null, null, null, "hot", 0, 10);

        assertNull(resp.getContent().get(0).getLogoUrl());
    }

    @Test
    void stats_batchAggregationAndZeroFill() {
        Category cat = Category.builder().id(1L).name("Skill").icon("🔧").build();
        Tool t1 = tool(1L, null, cat);
        Tool t2 = tool(2L, null, cat);
        when(toolRepository.findByFiltersOrderByHot(isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(t1, t2)));
        List<Object[]> favRows = List.<Object[]>of(new Object[]{1L, 3L});
        List<Object[]> dlRows = List.<Object[]>of(new Object[]{1L, 7L});
        when(unifiedFavoriteRepository.countByTargetTypeAndTargetIdIn(eq("TOOL"), anyCollection()))
                .thenReturn(favRows);
        when(toolFileRepository.sumDownloadCountGroupByToolId(anyCollection()))
                .thenReturn(dlRows);
        when(toolTagRepository.findByToolId(anyLong())).thenReturn(Collections.emptyList());

        PageResponse<ToolSummaryDTO> resp = newService().getTools(null, null, null, "hot", 0, 10);

        Map<Long, ToolSummaryDTO> byId = new java.util.HashMap<>();
        resp.getContent().forEach(dto -> byId.put(dto.getId(), dto));
        assertEquals(3, byId.get(1L).getFavoriteCount());
        assertEquals(7, byId.get(1L).getDownloadCount());
        assertEquals(0, byId.get(2L).getFavoriteCount());
        assertEquals(0, byId.get(2L).getDownloadCount());
    }
}
