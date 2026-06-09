package com.iaihub.toolbox.controller;

import com.iaihub.toolbox.dto.StatsDto;
import com.iaihub.toolbox.dto.ToolRankDto;
import com.iaihub.toolbox.dto.PostRankDto;
import com.iaihub.toolbox.service.OverviewService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OverviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OverviewService overviewService;

    @Test
    @WithMockUser
    void getStats_returnsUserCountPostCountToolCount() throws Exception {
        when(overviewService.getStats()).thenReturn(new StatsDto(100L, 200L, 50L));

        mockMvc.perform(get("/api/overview/stats"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userCount").value(100))
            .andExpect(jsonPath("$.postCount").value(200))
            .andExpect(jsonPath("$.toolCount").value(50));
    }

    @Test
    @WithMockUser
    void getToolRanks_returnsGroupedToolList() throws Exception {
        when(overviewService.getToolRanks()).thenReturn(List.of(
            new ToolRankDto(1L, "AI对话", "ChatGPT", new java.math.BigDecimal("999"))
        ));

        mockMvc.perform(get("/api/overview/tool-ranks"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithMockUser
    void getPostRanks_returnsGroupedPostList() throws Exception {
        when(overviewService.getPostRanks()).thenReturn(List.of(
            new PostRankDto(1L, "交流讨论", "AI时代产品经理该何去何从", new java.math.BigDecimal("50"))
        ));

        mockMvc.perform(get("/api/overview/post-ranks"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }
}