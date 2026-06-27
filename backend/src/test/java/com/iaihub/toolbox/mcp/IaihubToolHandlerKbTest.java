package com.iaihub.toolbox.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iaihub.toolbox.dto.LoginResponse;
import com.iaihub.toolbox.dto.kb.KbConfigRequest;
import com.iaihub.toolbox.dto.kb.KbCreateRequest;
import com.iaihub.toolbox.dto.kb.KbResponse;
import com.iaihub.toolbox.dto.kb.KbSearchRequest;
import com.iaihub.toolbox.dto.kb.KbSearchResultResponse;
import com.iaihub.toolbox.dto.kb.KbUpdateRequest;
import com.iaihub.toolbox.exception.ForbiddenException;
import com.iaihub.toolbox.exception.ResourceNotFoundException;
import com.iaihub.toolbox.model.User;
import com.iaihub.toolbox.service.McpSearchService;
import com.iaihub.toolbox.service.ToolFileService;
import com.iaihub.toolbox.service.ToolService;
import com.iaihub.toolbox.service.UserService;
import com.iaihub.toolbox.service.forum.ForumPostService;
import com.iaihub.toolbox.service.kb.KnowledgeBaseService;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IaihubToolHandlerKbTest {

    @Mock
    private McpSearchService searchService;
    @Mock
    private ToolService toolService;
    @Mock
    private ToolFileService toolFileService;
    @Mock
    private ForumPostService postService;
    @Mock
    private UserService userService;
    @Mock
    private KnowledgeBaseService knowledgeBaseService;

    private IaihubToolHandler handler;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        handler = new IaihubToolHandler(searchService, toolService, toolFileService,
                postService, userService, knowledgeBaseService, objectMapper);
    }

    // ── handleKbList ──────────────────────────────────────────

    @Test
    void handleKbList_defaultParams_returnsSuccess() {
        KbResponse kb = KbResponse.builder().id(1L).name("test-kb").build();
        Page<KbResponse> page = new PageImpl<>(List.of(kb));
        when(knowledgeBaseService.listKnowledgeBases(0, 20, null)).thenReturn(page);

        McpSchema.CallToolResult result = handler.handleKbList(null, null, null);

        assertFalse(result.isError());
        assertTrue(result.content().get(0) instanceof McpSchema.TextContent);
        String json = ((McpSchema.TextContent) result.content().get(0)).text();
        assertTrue(json.contains("test-kb"));
        assertTrue(json.contains("\"totalElements\":1"));
    }

    @Test
    void handleKbList_sortByHot_returnsSuccess() {
        Page<KbResponse> page = new PageImpl<>(Collections.emptyList());
        when(knowledgeBaseService.listKnowledgeBases(0, 10, "hot")).thenReturn(page);

        McpSchema.CallToolResult result = handler.handleKbList(0, 10, "hot");

        assertFalse(result.isError());
        verify(knowledgeBaseService).listKnowledgeBases(0, 10, "hot");
    }

    // ── handleKbSearch ────────────────────────────────────────

    @Test
    void handleKbSearch_success() {
        KbSearchResultResponse searchResult = KbSearchResultResponse.builder()
                .text("Spring Boot is a framework")
                .source("doc.txt")
                .score(0.85)
                .chunkIndex(0)
                .build();
        when(knowledgeBaseService.search(eq(1L), any(KbSearchRequest.class)))
                .thenReturn(List.of(searchResult));

        McpSchema.CallToolResult result = handler.handleKbSearch(1L, "Spring Boot", 5, null, 0);

        assertFalse(result.isError());
        String json = ((McpSchema.TextContent) result.content().get(0)).text();
        assertTrue(json.contains("Spring Boot is a framework"));
        assertTrue(json.contains("\"count\":1"));
    }

    @Test
    void handleKbSearch_kbNotFound_returnsError() {
        when(knowledgeBaseService.search(eq(999L), any(KbSearchRequest.class)))
                .thenThrow(new ResourceNotFoundException("知识库不存在", 999L));

        McpSchema.CallToolResult result = handler.handleKbSearch(999L, "test", 5, null, 0);

        assertTrue(result.isError());
        String json = ((McpSchema.TextContent) result.content().get(0)).text();
        assertTrue(json.contains("不存在"));
    }

    // ── handleKbCreate ────────────────────────────────────────

    @Test
    void handleKbCreate_success() {
        mockLogin("wangbao", "123456", 1L, "USER");
        KbResponse kbResponse = KbResponse.builder().id(1L).name("new-kb").build();
        when(knowledgeBaseService.createKnowledgeBase(any(KbCreateRequest.class), any(User.class)))
                .thenReturn(kbResponse);

        McpSchema.CallToolResult result = handler.handleKbCreate(
                "new-kb", "description", "structural", 800, 50, true, "wangbao", "123456");

        assertFalse(result.isError());
        String json = ((McpSchema.TextContent) result.content().get(0)).text();
        assertTrue(json.contains("new-kb"));
    }

    @Test
    void handleKbCreate_authFailed_returnsError() {
        when(userService.login(any())).thenThrow(new RuntimeException("认证失败"));

        McpSchema.CallToolResult result = handler.handleKbCreate(
                "new-kb", null, null, null, null, null, "baduser", "badpass");

        assertTrue(result.isError());
        String json = ((McpSchema.TextContent) result.content().get(0)).text();
        assertTrue(json.contains("认证失败"));
    }

    // ── handleKbUpdate ────────────────────────────────────────

    @Test
    void handleKbUpdate_nameAndDescription_success() {
        mockLogin("wangbao", "123456", 1L, "USER");
        KbResponse updated = KbResponse.builder().id(1L).name("updated-kb").description("new desc").build();
        when(knowledgeBaseService.updateKnowledgeBase(eq(1L), any(KbUpdateRequest.class), any(User.class)))
                .thenReturn(updated);

        McpSchema.CallToolResult result = handler.handleKbUpdate(
                1L, "updated-kb", "new desc", null, null, null, null, "wangbao", "123456");

        assertFalse(result.isError());
        String json = ((McpSchema.TextContent) result.content().get(0)).text();
        assertTrue(json.contains("updated-kb"));
        verify(knowledgeBaseService).updateKnowledgeBase(eq(1L), any(KbUpdateRequest.class), any(User.class));
        verify(knowledgeBaseService, never()).updateConfig(anyLong(), any(KbConfigRequest.class), any(User.class));
    }

    @Test
    void handleKbUpdate_configParams_success() {
        mockLogin("wangbao", "123456", 1L, "USER");
        KbResponse kbResponse = KbResponse.builder().id(1L).name("my-kb").build();
        when(knowledgeBaseService.updateConfig(eq(1L), any(KbConfigRequest.class), any(User.class)))
                .thenReturn(Map.of("chunk_size", 600));
        when(knowledgeBaseService.getKnowledgeBase(1L)).thenReturn(kbResponse);

        McpSchema.CallToolResult result = handler.handleKbUpdate(
                1L, null, null, null, 600, null, null, "wangbao", "123456");

        assertFalse(result.isError());
        verify(knowledgeBaseService, never()).updateKnowledgeBase(anyLong(), any(KbUpdateRequest.class), any(User.class));
        verify(knowledgeBaseService).updateConfig(eq(1L), any(KbConfigRequest.class), any(User.class));
    }

    @Test
    void handleKbUpdate_bothNameAndConfig_success() {
        mockLogin("wangbao", "123456", 1L, "USER");
        KbResponse updated = KbResponse.builder().id(1L).name("renamed").build();
        when(knowledgeBaseService.updateKnowledgeBase(eq(1L), any(KbUpdateRequest.class), any(User.class)))
                .thenReturn(updated);
        when(knowledgeBaseService.updateConfig(eq(1L), any(KbConfigRequest.class), any(User.class)))
                .thenReturn(Map.of("chunk_size", 600));

        McpSchema.CallToolResult result = handler.handleKbUpdate(
                1L, "renamed", null, null, 600, null, null, "wangbao", "123456");

        assertFalse(result.isError());
        verify(knowledgeBaseService).updateKnowledgeBase(eq(1L), any(KbUpdateRequest.class), any(User.class));
        verify(knowledgeBaseService).updateConfig(eq(1L), any(KbConfigRequest.class), any(User.class));
    }

    @Test
    void handleKbUpdate_notOwner_returnsError() {
        mockLogin("other", "123456", 2L, "USER");
        when(knowledgeBaseService.updateKnowledgeBase(eq(1L), any(KbUpdateRequest.class), any(User.class)))
                .thenThrow(new ForbiddenException("无权操作此知识库"));

        McpSchema.CallToolResult result = handler.handleKbUpdate(
                1L, "hacked", null, null, null, null, null, "other", "123456");

        assertTrue(result.isError());
        String json = ((McpSchema.TextContent) result.content().get(0)).text();
        assertTrue(json.contains("无权"));
    }

    // ── handleKbDelete ────────────────────────────────────────

    @Test
    void handleKbDelete_success() {
        mockLogin("wangbao", "123456", 1L, "USER");
        doNothing().when(knowledgeBaseService).deleteKnowledgeBase(eq(1L), any(User.class));

        McpSchema.CallToolResult result = handler.handleKbDelete(1L, "wangbao", "123456");

        assertFalse(result.isError());
        String json = ((McpSchema.TextContent) result.content().get(0)).text();
        assertTrue(json.contains("\"deleted\":true"));
    }

    @Test
    void handleKbDelete_notOwner_returnsError() {
        mockLogin("other", "123456", 2L, "USER");
        doThrow(new ForbiddenException("无权操作此知识库"))
                .when(knowledgeBaseService).deleteKnowledgeBase(eq(1L), any(User.class));

        McpSchema.CallToolResult result = handler.handleKbDelete(1L, "other", "123456");

        assertTrue(result.isError());
        String json = ((McpSchema.TextContent) result.content().get(0)).text();
        assertTrue(json.contains("无权"));
    }

    // ── handleKbUploadDocument ────────────────────────────────

    @Test
    void handleKbUploadDocument_success() {
        KbResponse kb = KbResponse.builder().id(1L).name("my-kb").build();
        when(knowledgeBaseService.getKnowledgeBase(1L)).thenReturn(kb);

        McpSchema.CallToolResult result = handler.handleKbUploadDocument(1L);

        assertFalse(result.isError());
        String json = ((McpSchema.TextContent) result.content().get(0)).text();
        assertTrue(json.contains("/api/v1/knowledge/1/documents"));
        assertTrue(json.contains("multipart/form-data"));
    }

    @Test
    void handleKbUploadDocument_kbNotFound_returnsError() {
        when(knowledgeBaseService.getKnowledgeBase(999L))
                .thenThrow(new ResourceNotFoundException("知识库不存在", 999L));

        McpSchema.CallToolResult result = handler.handleKbUploadDocument(999L);

        assertTrue(result.isError());
        String json = ((McpSchema.TextContent) result.content().get(0)).text();
        assertTrue(json.contains("不存在"));
    }

    // ── Helper ────────────────────────────────────────────────

    private void mockLogin(String username, String password, Long userId, String role) {
        LoginResponse.UserDTO userDTO = LoginResponse.UserDTO.builder()
                .id(userId)
                .username(username)
                .role(role)
                .build();
        LoginResponse loginResponse = LoginResponse.builder()
                .accessToken("mock-token")
                .user(userDTO)
                .build();
        when(userService.login(any())).thenReturn(loginResponse);
    }
}
