package com.iaihub.toolbox.controller;

import com.iaihub.toolbox.dto.StatsDto;
import com.iaihub.toolbox.service.OverviewService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

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
        mockMvc.perform(get("/api/overview/stats"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userCount").exists())
            .andExpect(jsonPath("$.postCount").exists())
            .andExpect(jsonPath("$.toolCount").exists());
    }
}