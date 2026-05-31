package com.iaihub.toolbox.mcp;

import com.iaihub.toolbox.dto.PostSearchResult;
import com.iaihub.toolbox.model.forum.ForumPost;
import com.iaihub.toolbox.model.ToolFile;
import com.iaihub.toolbox.service.McpSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MCP 工具处理器
 * 处理帖子检索和工具文件查询
 */
@Component
public class McpToolHandler {

    private static final Logger logger = LoggerFactory.getLogger(McpToolHandler.class);

    private final McpSearchService searchService;
    private final McpResourceHandler resourceHandler;

    public McpToolHandler(McpSearchService searchService, McpResourceHandler resourceHandler) {
        this.searchService = searchService;
        this.resourceHandler = resourceHandler;
    }

    /**
     * 列出所有可用工具
     */
    public List<Map<String, Object>> listTools() {
        List<Map<String, Object>> tools = new ArrayList<>();

        // 工具搜索
        tools.add(Map.of(
                "name", "h3_coding_hub_tool_search",
                "description", "Search tools in the tool square",
                "inputSchema", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "query", Map.of("type", "string", "description", "Search query"),
                                "category", Map.of("type", "string", "description", "Category filter"),
                                "limit", Map.of("type", "integer", "description", "Result limit", "default", 20)
                        )
                )
        ));

        // 工具详情
        tools.add(Map.of(
                "name", "h3_coding_hub_tool_get",
                "description", "Get tool details including full markdown documentation",
                "inputSchema", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "toolId", Map.of("type", "integer", "description", "Tool ID")
                        ),
                        "required", List.of("toolId")
                )
        ));

        // 工具文件列表
        tools.add(Map.of(
                "name", "h3_coding_hub_tool_files",
                "description", "Get tool file download information",
                "inputSchema", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "toolId", Map.of("type", "integer", "description", "Tool ID")
                        ),
                        "required", List.of("toolId")
                )
        ));

        // 帖子搜索
        tools.add(Map.of(
                "name", "h3_coding_hub_post_search",
                "description", "Search posts in the community",
                "inputSchema", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "query", Map.of("type", "string", "description", "Search query"),
                                "limit", Map.of("type", "integer", "description", "Result limit", "default", 20)
                        )
                )
        ));

        // 帖子详情
        tools.add(Map.of(
                "name", "h3_coding_hub_post_get",
                "description", "Get post content including full markdown",
                "inputSchema", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "postId", Map.of("type", "integer", "description", "Post ID")
                        ),
                        "required", List.of("postId")
                )
        ));

        return tools;
    }

    /**
     * 调用工具
     */
    public Map<String, Object> callTool(String toolName, Map<String, Object> arguments) {
        logger.info("Calling tool: {}", toolName);

        Map<String, Object> content = new HashMap<>();

        switch (toolName) {
            case "h3_coding_hub_tool_search":
                content = handleToolSearch(arguments);
                break;
            case "h3_coding_hub_tool_get":
                content = handleToolGet(arguments);
                break;
            case "h3_coding_hub_tool_files":
                content = handleToolFiles(arguments);
                break;
            case "h3_coding_hub_post_search":
                content = handlePostSearch(arguments);
                break;
            case "h3_coding_hub_post_get":
                content = handlePostGet(arguments);
                break;
            default:
                throw new IllegalArgumentException("Unknown tool: " + toolName);
        }

        return Map.of("content", List.of(Map.of("type", "text", "text", toJson(content))));
    }

    private Map<String, Object> handleToolSearch(Map<String, Object> arguments) {
        String query = (String) arguments.get("query");
        String category = (String) arguments.get("category");
        Integer limit = arguments.get("limit") != null ? ((Number) arguments.get("limit")).intValue() : 20;

        var results = searchService.searchTools(query, category, limit);
        return Map.of("tools", results, "count", results.size());
    }

    private Map<String, Object> handleToolGet(Map<String, Object> arguments) {
        Long toolId = ((Number) arguments.get("toolId")).longValue();
        var tool = searchService.getToolById(toolId);

        if (tool == null) {
            return Map.of("error", "Tool not found", "toolId", toolId);
        }

        return Map.of(
                "id", tool.getId(),
                "name", tool.getName(),
                "content", tool.getContent() != null ? tool.getContent() : "",
                "category", tool.getCategory() != null ? tool.getCategory().getName() : ""
        );
    }

    private Map<String, Object> handleToolFiles(Map<String, Object> arguments) {
        Long toolId = ((Number) arguments.get("toolId")).longValue();
        var files = searchService.getToolFiles(toolId);

        if (files.isEmpty()) {
            return Map.of("files", List.of(), "count", 0, "toolId", toolId);
        }

        List<Map<String, Object>> fileList = new ArrayList<>();
        for (ToolFile file : files) {
            fileList.add(Map.of(
                    "fileName", file.getOriginalName(),
                    "fileSize", file.getFileSize(),
                    "downloadUrl", "/api/files/download/" + file.getId(),
                    "createdAt", file.getCreatedAt() != null ? file.getCreatedAt().toString() : ""
            ));
        }

        return Map.of("files", fileList, "count", fileList.size(), "toolId", toolId);
    }

    private Map<String, Object> handlePostSearch(Map<String, Object> arguments) {
        String query = (String) arguments.get("query");
        Integer limit = arguments.get("limit") != null ? ((Number) arguments.get("limit")).intValue() : 20;

        var results = searchService.searchPosts(query, limit);
        return Map.of("posts", results, "count", results.size());
    }

    private Map<String, Object> handlePostGet(Map<String, Object> arguments) {
        Long postId = ((Number) arguments.get("postId")).longValue();
        var post = searchService.getPostById(postId);

        if (post == null) {
            return Map.of("error", "Post not found", "postId", postId);
        }

        return Map.of(
                "id", post.getId(),
                "title", post.getTitle(),
                "content", post.getContent() != null ? post.getContent() : "",
                "authorId", post.getAuthorId() != null ? post.getAuthorId() : 0,
                "createdAt", post.getCreatedAt() != null ? post.getCreatedAt().toString() : ""
        );
    }

    private String toJson(Object obj) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(obj);
        } catch (Exception e) {
            logger.error("Failed to serialize to JSON", e);
            return "{}";
        }
    }
}