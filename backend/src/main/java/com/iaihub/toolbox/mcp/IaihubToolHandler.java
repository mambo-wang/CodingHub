package com.iaihub.toolbox.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iaihub.toolbox.dto.PostSearchResult;
import com.iaihub.toolbox.dto.ToolSearchResult;
import com.iaihub.toolbox.model.Tool;
import com.iaihub.toolbox.model.ToolFile;
import com.iaihub.toolbox.model.forum.ForumPost;
import com.iaihub.toolbox.service.McpSearchService;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * MCP 工具处理器 - 实现 SDK 工具处理接口
 *
 * <p>处理以下工具调用：
 * <ul>
 *   <li>h3_coding_hub_tool_search - 搜索工具列表</li>
 *   <li>h3_coding_hub_tool_get - 获取工具详情</li>
 *   <li>h3_coding_hub_tool_files - 获取工具文件</li>
 *   <li>h3_coding_hub_post_search - 搜索帖子</li>
 *   <li>h3_coding_hub_post_get - 获取帖子详情</li>
 * </ul>
 */
@Component
public class IaihubToolHandler {

    private static final Logger logger = LoggerFactory.getLogger(IaihubToolHandler.class);

    private final McpSearchService searchService;
    private final ObjectMapper objectMapper;

    public IaihubToolHandler(McpSearchService searchService, ObjectMapper objectMapper) {
        this.searchService = searchService;
        this.objectMapper = objectMapper;
    }

    /**
     * 处理工具搜索
     */
    public McpSchema.CallToolResult handleToolSearch(String query, String category, Integer limit) {
        logger.info("MCP tool search: query={}, category={}, limit={}", query, category, limit);
        try {
            List<ToolSearchResult> results = searchService.searchTools(query, category, limit);
            String json = toJson(new ToolSearchResponse(results));
            return new McpSchema.CallToolResult(List.of(new McpSchema.TextContent(json)), false);
        } catch (Exception e) {
            logger.error("Error searching tools", e);
            return errorResult("搜索工具失败: " + e.getMessage());
        }
    }

    /**
     * 处理获取工具详情
     */
    public McpSchema.CallToolResult handleToolGet(Long toolId) {
        logger.info("MCP get tool: toolId={}", toolId);
        try {
            Tool tool = searchService.getToolById(toolId);
            if (tool == null) {
                return errorResult("工具不存在: " + toolId);
            }
            String json = toJson(new ToolDetailResponse(
                    tool.getId(),
                    tool.getName(),
                    tool.getContent() != null ? tool.getContent() : "",
                    tool.getCategory() != null ? tool.getCategory().getName() : ""
            ));
            return new McpSchema.CallToolResult(List.of(new McpSchema.TextContent(json)), false);
        } catch (Exception e) {
            logger.error("Error getting tool", e);
            return errorResult("获取工具详情失败: " + e.getMessage());
        }
    }

    /**
     * 处理获取工具文件列表
     */
    public McpSchema.CallToolResult handleToolFiles(Long toolId) {
        logger.info("MCP get tool files: toolId={}", toolId);
        try {
            List<ToolFile> files = searchService.getToolFiles(toolId);
            List<FileInfo> fileList = new ArrayList<>();
            for (ToolFile file : files) {
                fileList.add(new FileInfo(
                        file.getOriginalName(),
                        file.getFileSize(),
                        "/api/files/download/" + file.getId(),
                        file.getCreatedAt() != null ? file.getCreatedAt().toString() : ""
                ));
            }
            String json = toJson(new ToolFilesResponse(fileList, toolId));
            return new McpSchema.CallToolResult(List.of(new McpSchema.TextContent(json)), false);
        } catch (Exception e) {
            logger.error("Error getting tool files", e);
            return errorResult("获取工具文件失败: " + e.getMessage());
        }
    }

    /**
     * 处理帖子搜索
     */
    public McpSchema.CallToolResult handlePostSearch(String query, Integer limit) {
        logger.info("MCP post search: query={}, limit={}", query, limit);
        try {
            List<PostSearchResult> results = searchService.searchPosts(query, limit);
            String json = toJson(new PostSearchResponse(results));
            return new McpSchema.CallToolResult(List.of(new McpSchema.TextContent(json)), false);
        } catch (Exception e) {
            logger.error("Error searching posts", e);
            return errorResult("搜索帖子失败: " + e.getMessage());
        }
    }

    /**
     * 处理获取帖子详情
     */
    public McpSchema.CallToolResult handlePostGet(Long postId) {
        logger.info("MCP get post: postId={}", postId);
        try {
            ForumPost post = searchService.getPostById(postId);
            if (post == null) {
                return errorResult("帖子不存在: " + postId);
            }
            String json = toJson(new PostDetailResponse(
                    post.getId(),
                    post.getTitle(),
                    post.getContent() != null ? post.getContent() : "",
                    post.getAuthorId() != null ? post.getAuthorId() : 0,
                    post.getCreatedAt() != null ? post.getCreatedAt().toString() : ""
            ));
            return new McpSchema.CallToolResult(List.of(new McpSchema.TextContent(json)), false);
        } catch (Exception e) {
            logger.error("Error getting post", e);
            return errorResult("获取帖子详情失败: " + e.getMessage());
        }
    }

    /**
     * 处理文件下载
     * 返回文件的下载链接和基本信息
     */
    public McpSchema.CallToolResult handleToolDownload(Long toolId, Long fileId) {
        logger.info("MCP download file: toolId={}, fileId={}", toolId, fileId);
        try {
            com.iaihub.toolbox.model.ToolFile file = searchService.getToolFile(toolId, fileId);
            if (file == null) {
                return errorResult("文件不存在: toolId=" + toolId + ", fileId=" + fileId);
            }
            String downloadUrl = "/api/v1/tools/" + toolId + "/files/" + fileId + "/download";
            String json = toJson(new FileDownloadResponse(
                    file.getId(),
                    file.getOriginalName(),
                    file.getFileSize(),
                    file.getContentType() != null ? file.getContentType() : "application/octet-stream",
                    downloadUrl,
                    file.getCreatedAt() != null ? file.getCreatedAt().toString() : ""
            ));
            return new McpSchema.CallToolResult(List.of(new McpSchema.TextContent(json)), false);
        } catch (Exception e) {
            logger.error("Error downloading file", e);
            return errorResult("获取下载链接失败: " + e.getMessage());
        }
    }

    private McpSchema.CallToolResult errorResult(String message) {
        return new McpSchema.CallToolResult(List.of(new McpSchema.TextContent(
                toJson(new ErrorResponse(message))
        )), true);
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            logger.error("Failed to serialize to JSON", e);
            return "{}";
        }
    }

    // DTO 类
    private static class ToolSearchResponse {
        public List<ToolSearchResult> tools;
        public int count;
        public ToolSearchResponse(List<ToolSearchResult> tools) {
            this.tools = tools;
            this.count = tools.size();
        }
    }

    private static class ToolDetailResponse {
        public Long id;
        public String name;
        public String content;
        public String category;
        public ToolDetailResponse(Long id, String name, String content, String category) {
            this.id = id;
            this.name = name;
            this.content = content;
            this.category = category;
        }
    }

    private static class FileInfo {
        public String fileName;
        public Long fileSize;
        public String downloadUrl;
        public String createdAt;
        public FileInfo(String fileName, Long fileSize, String downloadUrl, String createdAt) {
            this.fileName = fileName;
            this.fileSize = fileSize;
            this.downloadUrl = downloadUrl;
            this.createdAt = createdAt;
        }
    }

    private static class ToolFilesResponse {
        public List<FileInfo> files;
        public int count;
        public Long toolId;
        public ToolFilesResponse(List<FileInfo> files, Long toolId) {
            this.files = files;
            this.count = files.size();
            this.toolId = toolId;
        }
    }

    private static class PostSearchResponse {
        public List<PostSearchResult> posts;
        public int count;
        public PostSearchResponse(List<PostSearchResult> posts) {
            this.posts = posts;
            this.count = posts.size();
        }
    }

    private static class PostDetailResponse {
        public Long id;
        public String title;
        public String content;
        public Long authorId;
        public String createdAt;
        public PostDetailResponse(Long id, String title, String content, Long authorId, String createdAt) {
            this.id = id;
            this.title = title;
            this.content = content;
            this.authorId = authorId;
            this.createdAt = createdAt;
        }
    }

    private static class ErrorResponse {
        public String error;
        public ErrorResponse(String error) {
            this.error = error;
        }
    }

    private static class FileDownloadResponse {
        public Long fileId;
        public String fileName;
        public Long fileSize;
        public String contentType;
        public String downloadUrl;
        public String createdAt;
        public FileDownloadResponse(Long fileId, String fileName, Long fileSize, String contentType, String downloadUrl, String createdAt) {
            this.fileId = fileId;
            this.fileName = fileName;
            this.fileSize = fileSize;
            this.contentType = contentType;
            this.downloadUrl = downloadUrl;
            this.createdAt = createdAt;
        }
    }
}