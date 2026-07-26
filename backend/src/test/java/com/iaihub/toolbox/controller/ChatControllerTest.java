package com.iaihub.toolbox.controller;

import com.iaihub.toolbox.dto.ChatMessageDTO;
import com.iaihub.toolbox.service.ChatService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ChatService chatService;

    @Test
    void getHistory_returnsMessages() throws Exception {
        List<ChatMessageDTO> messages = List.of(
                ChatMessageDTO.builder()
                        .id(1L).roomId("global").userId(1L)
                        .displayName("User1").content("Hello")
                        .status("ACTIVE").createdAt(LocalDateTime.now())
                        .guest(false).build()
        );
        when(chatService.getHistory("global", 50)).thenReturn(messages);

        mockMvc.perform(get("/api/v1/chat/messages")
                        .param("roomId", "global")
                        .param("limit", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].content").value("Hello"));
    }

    @Test
    void deleteMessage_noAuth_returns401or403() throws Exception {
        mockMvc.perform(delete("/api/v1/chat/messages/1").with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteMessage_admin_succeeds() throws Exception {
        mockMvc.perform(delete("/api/v1/chat/messages/1").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(chatService).softDelete(1L);
    }

    @Test
    @WithMockUser(roles = "USER")
    void deleteMessage_regularUser_returns403() throws Exception {
        mockMvc.perform(delete("/api/v1/chat/messages/1").with(csrf()))
                .andExpect(status().isForbidden());
    }
}
