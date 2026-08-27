package com.iaihub.toolbox.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iaihub.toolbox.dto.CreateToolRequest;
import com.iaihub.toolbox.dto.LoginResponse;
import com.iaihub.toolbox.dto.ToolDetailDTO;
import com.iaihub.toolbox.dto.ToolSummaryDTO;
import com.iaihub.toolbox.dto.UpdateToolRequest;
import com.iaihub.toolbox.model.Role;
import com.iaihub.toolbox.model.User;
import com.iaihub.toolbox.service.McpSearchService;
import com.iaihub.toolbox.service.RagApiClient;
import com.iaihub.toolbox.service.ToolFileService;
import com.iaihub.toolbox.service.ToolService;
import com.iaihub.toolbox.service.UserService;
import com.iaihub.toolbox.service.forum.ForumPostService;
import com.iaihub.toolbox.service.kb.KnowledgeBaseService;
import com.iaihub.toolbox.service.tag.TagService;
import com.iaihub.toolbox.service.plugin.PluginService;
import com.iaihub.toolbox.model.tag.TagType;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IaihubToolHandlerMcpTest {

    @Mock private McpSearchService searchService;
    @Mock private ToolService toolService;
    @Mock private ToolFileService toolFileService;
    @Mock private ForumPostService postService;
    @Mock private UserService userService;
    @Mock private KnowledgeBaseService knowledgeBaseService;
    @Mock private RagApiClient ragApiClient;
    @Mock private McpNotificationService mcpNotificationService;
    @Mock private TagService tagService;
    @Mock private PluginService pluginService;

    private IaihubToolHandler handler;
    private ObjectMapper objectMapper;

    private LoginResponse loginResponse;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        handler = new IaihubToolHandler(searchService, toolService, toolFileService,
                postService, userService, knowledgeBaseService, ragApiClient,
                objectMapper, mcpNotificationService, tagService, pluginService, "");

        LoginResponse.UserDTO userDto = LoginResponse.UserDTO.builder()
                .id(1L).username("testuser").role("USER").build();
        loginResponse = LoginResponse.builder()
                .accessToken("jwt-token").user(userDto).build();
        when(userService.login(any())).thenReturn(loginResponse);
    }

    @Test
    void handleToolCreate_withDescriptionAndTags() {
        // Setup: tag resolution returns IDs
        when(tagService.resolveOrCreateTags(List.of("Python", "CLI"), TagType.TOOL))
                .thenReturn(List.of(5L, 8L));

        // Setup: tool creation returns a summary
        ToolSummaryDTO created = ToolSummaryDTO.builder()
                .id(42L).name("TestTool").version("1.0.0")
                .description("A test tool").categoryName("DevTools").build();
        when(toolService.createTool(any(CreateToolRequest.class), eq(1L))).thenReturn(created);

        // Execute
        McpSchema.CallToolResult result = handler.handleToolCreate(
                "TestTool", 1L, "Content", "1.0.0",
                "A test tool", List.of("Python", "CLI"),
                "testuser", "123456");

        // Verify: CreateToolRequest has description and tagIds
        ArgumentCaptor<CreateToolRequest> captor = ArgumentCaptor.forClass(CreateToolRequest.class);
        verify(toolService).createTool(captor.capture(), eq(1L));

        CreateToolRequest request = captor.getValue();
        assertEquals("A test tool", request.getDescription());
        assertEquals(List.of(5L, 8L), request.getTagIds());

        // Verify result
        assertFalse(result.isError());
    }

    @Test
    void handleToolCreate_withoutDescriptionAndTags() {
        ToolSummaryDTO created = ToolSummaryDTO.builder()
                .id(42L).name("TestTool").version("1.0.0").categoryName("DevTools").build();
        when(toolService.createTool(any(CreateToolRequest.class), eq(1L))).thenReturn(created);

        McpSchema.CallToolResult result = handler.handleToolCreate(
                "TestTool", 1L, "Content", "1.0.0",
                null, null,
                "testuser", "123456");

        ArgumentCaptor<CreateToolRequest> captor = ArgumentCaptor.forClass(CreateToolRequest.class);
        verify(toolService).createTool(captor.capture(), eq(1L));

        CreateToolRequest request = captor.getValue();
        assertNull(request.getDescription());
        assertNull(request.getTagIds());

        verify(tagService, never()).resolveOrCreateTags(any(), any());
        assertFalse(result.isError());
    }

    @Test
    void handleToolModify_withDescriptionAndTags() {
        ToolDetailDTO updated = ToolDetailDTO.builder()
                .id(42L).name("TestTool").version("1.0.1").categoryName("DevTools")
                .description("New desc").build();
        when(toolService.updateTool(eq(42L), any(UpdateToolRequest.class), any(User.class))).thenReturn(updated);
        when(tagService.resolveOrCreateTags(List.of("Java"), TagType.TOOL)).thenReturn(List.of(3L));

        McpSchema.CallToolResult result = handler.handleToolModify(
                42L, null, null, null, "1.0.1",
                "New desc", List.of("Java"),
                "testuser", "123456");

        ArgumentCaptor<UpdateToolRequest> captor = ArgumentCaptor.forClass(UpdateToolRequest.class);
        verify(toolService).updateTool(eq(42L), captor.capture(), any(User.class));

        UpdateToolRequest request = captor.getValue();
        assertEquals("New desc", request.getDescription());
        assertEquals(List.of(3L), request.getTagIds());

        assertFalse(result.isError());
    }

    @Test
    void handleToolModify_emptyTagsClearsAll() {
        ToolDetailDTO updated = ToolDetailDTO.builder()
                .id(42L).name("TestTool").version("1.0.1").categoryName("DevTools").build();
        when(toolService.updateTool(eq(42L), any(UpdateToolRequest.class), any(User.class))).thenReturn(updated);

        McpSchema.CallToolResult result = handler.handleToolModify(
                42L, null, null, null, "1.0.1",
                null, List.of(),
                "testuser", "123456");

        ArgumentCaptor<UpdateToolRequest> captor = ArgumentCaptor.forClass(UpdateToolRequest.class);
        verify(toolService).updateTool(eq(42L), captor.capture(), any(User.class));

        UpdateToolRequest request = captor.getValue();
        assertNotNull(request.getTagIds());
        assertTrue(request.getTagIds().isEmpty());

        verify(tagService, never()).resolveOrCreateTags(any(), any());
        assertFalse(result.isError());
    }

    @Test
    void handleToolModify_nullTagsKeepsUnchanged() {
        ToolDetailDTO updated = ToolDetailDTO.builder()
                .id(42L).name("TestTool").version("1.0.1").categoryName("DevTools").build();
        when(toolService.updateTool(eq(42L), any(UpdateToolRequest.class), any(User.class))).thenReturn(updated);

        handler.handleToolModify(
                42L, null, null, null, "1.0.1",
                null, null,
                "testuser", "123456");

        ArgumentCaptor<UpdateToolRequest> captor = ArgumentCaptor.forClass(UpdateToolRequest.class);
        verify(toolService).updateTool(eq(42L), captor.capture(), any(User.class));

        UpdateToolRequest request = captor.getValue();
        assertNull(request.getTagIds());

        verify(tagService, never()).resolveOrCreateTags(any(), any());
    }
}
