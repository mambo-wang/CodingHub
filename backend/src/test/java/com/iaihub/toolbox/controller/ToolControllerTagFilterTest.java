package com.iaihub.toolbox.controller;

import com.iaihub.toolbox.dto.PageResponse;
import com.iaihub.toolbox.dto.ToolSummaryDTO;
import com.iaihub.toolbox.mcp.McpNotificationService;
import com.iaihub.toolbox.service.ToolService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Task 3.2: ToolController 标签筛选集成测试（MockMvc standalone）
 * 验证 GET /api/v1/tools?tagId=3 返回正确结果、不带 tagId 行为不变
 */
@ExtendWith(MockitoExtension.class)
class ToolControllerTagFilterTest {

    @Mock
    private ToolService toolService;

    @Mock
    private McpNotificationService mcpNotificationService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ToolController controller = new ToolController(toolService, mcpNotificationService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void getTools_withTagId_passesTagIdToService() throws Exception {
        // Given
        PageResponse<ToolSummaryDTO> response = PageResponse.<ToolSummaryDTO>builder()
                .content(Collections.emptyList()).totalElements(0).totalPages(0)
                .page(0).size(12).build();
        when(toolService.getTools(isNull(), isNull(), eq(3L), eq("hot"), eq(0), eq(12)))
                .thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/v1/tools").param("tagId", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(toolService).getTools(isNull(), isNull(), eq(3L), eq("hot"), eq(0), eq(12));
    }

    @Test
    void getTools_withoutTagId_passesNullTagId() throws Exception {
        // Given
        PageResponse<ToolSummaryDTO> response = PageResponse.<ToolSummaryDTO>builder()
                .content(Collections.emptyList()).totalElements(0).totalPages(0)
                .page(0).size(12).build();
        when(toolService.getTools(isNull(), isNull(), isNull(), eq("hot"), eq(0), eq(12)))
                .thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/v1/tools"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(toolService).getTools(isNull(), isNull(), isNull(), eq("hot"), eq(0), eq(12));
    }

    @Test
    void getTools_withTagIdAndCategoryAndKeyword_allParamsPassed() throws Exception {
        // Given
        PageResponse<ToolSummaryDTO> response = PageResponse.<ToolSummaryDTO>builder()
                .content(Collections.emptyList()).totalElements(0).totalPages(0)
                .page(0).size(12).build();
        when(toolService.getTools(eq(1L), eq("test"), eq(3L), eq("hot"), eq(0), eq(12)))
                .thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/v1/tools")
                        .param("categoryId", "1")
                        .param("keyword", "test")
                        .param("tagId", "3"))
                .andExpect(status().isOk());

        verify(toolService).getTools(eq(1L), eq("test"), eq(3L), eq("hot"), eq(0), eq(12));
    }
}
