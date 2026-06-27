package com.iaihub.toolbox.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iaihub.toolbox.dto.PostSearchResult;
import com.iaihub.toolbox.dto.ToolSearchResult;
import com.iaihub.toolbox.model.Role;
import com.iaihub.toolbox.model.Tool;
import com.iaihub.toolbox.model.ToolFile;
import com.iaihub.toolbox.model.User;
import com.iaihub.toolbox.model.forum.ForumPost;
import com.iaihub.toolbox.service.McpSearchService;
import com.iaihub.toolbox.service.ToolFileService;
import com.iaihub.toolbox.service.ToolService;
import com.iaihub.toolbox.service.forum.ForumPostService;
import com.iaihub.toolbox.service.UserService;
import com.iaihub.toolbox.dto.CreateToolRequest;
import com.iaihub.toolbox.dto.LoginRequest;
import com.iaihub.toolbox.dto.LoginResponse;
import com.iaihub.toolbox.dto.ToolDetailDTO;
import com.iaihub.toolbox.dto.ToolSummaryDTO;
import com.iaihub.toolbox.dto.UpdateToolRequest;
import com.iaihub.toolbox.dto.forum.ForumPostCreateRequest;
import com.iaihub.toolbox.dto.forum.ForumPostDTO;
import com.iaihub.toolbox.dto.kb.KbConfigRequest;
import com.iaihub.toolbox.dto.kb.KbCreateRequest;
import com.iaihub.toolbox.dto.kb.KbResponse;
import com.iaihub.toolbox.dto.kb.KbSearchRequest;
import com.iaihub.toolbox.dto.kb.KbSearchResultResponse;
import com.iaihub.toolbox.dto.kb.KbUpdateRequest;
import com.iaihub.toolbox.service.kb.KnowledgeBaseService;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
 *   <li>h3_coding_hub_tool_download - 获取文件下载链接</li>
 *   <li>h3_coding_hub_tool_create - 创建工具（需要认证）</li>
 *   <li>h3_coding_hub_post_create - 创建帖子（需要认证）</li>
 *   <li>h3_coding_hub_tool_file_upload - 获取文件上传接口信息</li>
 *   <li>h3_coding_hub_tool_modify - 修改工具（需要认证）</li>
 *   <li>h3_coding_hub_tool_file_delete - 删除工具文件（需要认证）</li>
 *   <li>h3_coding_hub_kb_list - 获取知识库列表</li>
 *   <li>h3_coding_hub_kb_search - 语义搜索知识库内容</li>
 *   <li>h3_coding_hub_kb_create - 创建知识库（需要认证）</li>
 *   <li>h3_coding_hub_kb_update - 更新知识库（需要认证）</li>
 *   <li>h3_coding_hub_kb_delete - 删除知识库（需要认证）</li>
 *   <li>h3_coding_hub_kb_upload_document - 获取文档上传接口信息</li>
 * </ul>
 */
@Component
public class IaihubToolHandler {

    private static final Logger logger = LoggerFactory.getLogger(IaihubToolHandler.class);

    private final McpSearchService searchService;
    private final ToolService toolService;
    private final ToolFileService toolFileService;
    private final ForumPostService postService;
    private final UserService userService;
    private final KnowledgeBaseService knowledgeBaseService;
    private final ObjectMapper objectMapper;

    public IaihubToolHandler(McpSearchService searchService,
                             ToolService toolService,
                             ToolFileService toolFileService,
                             ForumPostService postService,
                             UserService userService,
                             KnowledgeBaseService knowledgeBaseService,
                             ObjectMapper objectMapper) {
        this.searchService = searchService;
        this.toolService = toolService;
        this.toolFileService = toolFileService;
        this.postService = postService;
        this.userService = userService;
        this.knowledgeBaseService = knowledgeBaseService;
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
            return successResult(json);
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
                    tool.getVersion() != null ? tool.getVersion() : "1.0.0",
                    tool.getContent() != null ? tool.getContent() : "",
                    tool.getCategory() != null ? tool.getCategory().getName() : ""
            ));
            return successResult(json);
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
                        "/api/v1/tools/" + toolId + "/files/" + file.getId() + "/download",
                        file.getCreatedAt() != null ? file.getCreatedAt().toString() : ""
                ));
            }
            String json = toJson(new ToolFilesResponse(fileList, toolId));
            return successResult(json);
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
            return successResult(json);
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
            return successResult(json);
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
            return successResult(json);
        } catch (Exception e) {
            logger.error("Error downloading file", e);
            return errorResult("获取下载链接失败: " + e.getMessage());
        }
    }

    /**
     * 处理创建工具（认证参数由 MCP 客户端传入）
     */
    public McpSchema.CallToolResult handleToolCreate(String name, Long categoryId, String content, String version,
                                                      String username, String password) {
        logger.info("MCP create tool: name={}, categoryId={}, version={}, username={}", name, categoryId, version, username);
        try {
            // 使用 MCP 客户端传入的账号密码登录
            LoginRequest loginRequest = LoginRequest.builder()
                    .username(username)
                    .password(password)
                    .build();
            LoginResponse loginResult = userService.login(loginRequest);
            Long userId = loginResult.getUser().getId();

            // 调用创建工具
            CreateToolRequest request = CreateToolRequest.builder()
                    .name(name)
                    .categoryId(categoryId)
                    .content(content)
                    .version(version)
                    .build();

            ToolSummaryDTO created = toolService.createTool(request, userId);
            String json = toJson(created);
            return successResult(json);
        } catch (Exception e) {
            logger.error("Error creating tool via MCP", e);
            return errorResult("创建工具失败: " + e.getMessage());
        }
    }

    /**
     * 处理创建帖子（认证参数由 MCP 客户端传入）
     */
    public McpSchema.CallToolResult handlePostCreate(String title, String content, Long categoryId,
                                                      String username, String password) {
        logger.info("MCP create post: title={}, categoryId={}, username={}", title, categoryId, username);
        try {
            // 使用 MCP 客户端传入的账号密码登录
            LoginRequest loginRequest = LoginRequest.builder()
                    .username(username)
                    .password(password)
                    .build();
            LoginResponse loginResult = userService.login(loginRequest);
            Long userId = loginResult.getUser().getId();

            // 调用创建帖子
            ForumPostCreateRequest request = new ForumPostCreateRequest(title, content, categoryId, null);
            ForumPostDTO created = postService.createPost(userId, request);
            String json = toJson(created);
            return successResult(json);
        } catch (Exception e) {
            logger.error("Error creating post via MCP", e);
            return errorResult("创建帖子失败: " + e.getMessage());
        }
    }

    /**
     * 处理获取文件上传信息（告知客户端 REST API 详情）
     */
    public McpSchema.CallToolResult handleToolFileUploadInfo(Long toolId) {
        logger.info("MCP file upload info: toolId={}", toolId);
        try {
            Tool tool = searchService.getToolById(toolId);
            if (tool == null) {
                return errorResult("工具不存在: " + toolId);
            }
            String json = toJson(new FileUploadInfoResponse(
                    toolId,
                    tool.getName(),
                    "/api/v1/tools/" + toolId + "/files",
                    "POST",
                    "multipart/form-data",
                    "files (必填, 文件列表), readme (可选, markdown文本)",
                    "50MB per file, 200MB total"
            ));
            return successResult(json);
        } catch (Exception e) {
            logger.error("Error getting file upload info", e);
            return errorResult("获取上传信息失败: " + e.getMessage());
        }
    }

    /**
     * 处理修改工具（认证参数由 MCP 客户端传入）
     */
    public McpSchema.CallToolResult handleToolModify(Long toolId, String name, Long categoryId,
                                                      String content, String version,
                                                      String username, String password) {
        logger.info("MCP modify tool: toolId={}, name={}, categoryId={}, version={}, username={}",
                toolId, name, categoryId, version, username);
        try {
            // 使用 MCP 客户端传入的账号密码登录
            LoginRequest loginRequest = LoginRequest.builder()
                    .username(username)
                    .password(password)
                    .build();
            LoginResponse loginResult = userService.login(loginRequest);
            // Build a User object with the necessary fields for permission check
            User mcpUser = User.builder()
                    .id(loginResult.getUser().getId())
                    .username(loginResult.getUser().getUsername())
                    .role(loginResult.getUser().getRole() != null ? Role.valueOf(loginResult.getUser().getRole()) : Role.USER)
                    .build();

            // 如果未传版本号，自动递增
            if (version == null || version.isBlank()) {
                Tool tool = searchService.getToolById(toolId);
                if (tool == null) {
                    return errorResult("工具不存在: " + toolId);
                }
                String currentVersion = tool.getVersion() != null ? tool.getVersion() : "1.0.0";
                version = incrementVersion(currentVersion);
                logger.info("Auto-incremented version: {} -> {}", currentVersion, version);
            }

            // 调用更新工具
            UpdateToolRequest request = UpdateToolRequest.builder()
                    .name(name != null && !name.isBlank() ? name : null)
                    .categoryId(categoryId)
                    .content(content != null && !content.isBlank() ? content : null)
                    .version(version)
                    .build();

            ToolDetailDTO updated = toolService.updateTool(toolId, request, mcpUser);
            String json = toJson(updated);
            return successResult(json);
        } catch (Exception e) {
            logger.error("Error modifying tool via MCP", e);
            return errorResult("修改工具失败: " + e.getMessage());
        }
    }

    /**
     * 处理删除工具文件（认证参数由 MCP 客户端传入）
     */
    public McpSchema.CallToolResult handleToolFileDelete(Long toolId, Long fileId,
                                                          String username, String password) {
        logger.info("MCP delete file: toolId={}, fileId={}", toolId, fileId);
        try {
            // 使用 MCP 客户端传入的账号密码登录
            LoginRequest loginRequest = LoginRequest.builder()
                    .username(username)
                    .password(password)
                    .build();
            LoginResponse loginResult = userService.login(loginRequest);
            Long userId = loginResult.getUser().getId();

            // 调用删除文件
            toolFileService.deleteToolFile(toolId, fileId, userId);
            String json = toJson(new FileDeleteResponse(toolId, fileId, true));
            return successResult(json);
        } catch (Exception e) {
            logger.error("Error deleting file via MCP", e);
            return errorResult("删除文件失败: " + e.getMessage());
        }
    }

    /**
     * 处理知识库列表
     */
    public McpSchema.CallToolResult handleKbList(Integer page, Integer size, String sortBy) {
        logger.info("MCP kb list: page={}, size={}, sortBy={}", page, size, sortBy);
        try {
            int p = page != null ? page : 0;
            int s = size != null ? size : 20;
            Page<KbResponse> kbPage = knowledgeBaseService.listKnowledgeBases(p, s, sortBy);
            String json = toJson(new KbListResponse(
                    kbPage.getContent(),
                    kbPage.getTotalElements(),
                    kbPage.getTotalPages(),
                    kbPage.getNumber(),
                    kbPage.getSize()
            ));
            return successResult(json);
        } catch (Exception e) {
            logger.error("Error listing knowledge bases", e);
            return errorResult("获取知识库列表失败: " + e.getMessage());
        }
    }

    /**
     * 处理知识库语义搜索
     */
    public McpSchema.CallToolResult handleKbSearch(Long kbId, String query, Integer topK,
                                                    Boolean rerank, Integer expandContext) {
        logger.info("MCP kb search: kbId={}, query={}, topK={}", kbId, query, topK);
        try {
            KbSearchRequest request = KbSearchRequest.builder()
                    .query(query)
                    .topK(topK != null ? topK : 5)
                    .rerank(rerank)
                    .expandContext(expandContext != null ? expandContext : 0)
                    .build();
            List<KbSearchResultResponse> results = knowledgeBaseService.search(kbId, request);
            String json = toJson(new KbSearchResponse(results));
            return successResult(json);
        } catch (Exception e) {
            logger.error("Error searching knowledge base", e);
            return errorResult("搜索知识库失败: " + e.getMessage());
        }
    }

    /**
     * 处理创建知识库（认证参数由 MCP 客户端传入）
     */
    public McpSchema.CallToolResult handleKbCreate(String name, String description, String chunkMode,
                                                    Integer chunkSize, Integer chunkOverlap, Boolean rerank,
                                                    String username, String password) {
        logger.info("MCP kb create: name={}, username={}", name, username);
        try {
            LoginRequest loginRequest = LoginRequest.builder()
                    .username(username)
                    .password(password)
                    .build();
            LoginResponse loginResult = userService.login(loginRequest);
            User user = User.builder()
                    .id(loginResult.getUser().getId())
                    .username(loginResult.getUser().getUsername())
                    .role(loginResult.getUser().getRole() != null ? Role.valueOf(loginResult.getUser().getRole()) : Role.USER)
                    .build();

            KbCreateRequest request = KbCreateRequest.builder()
                    .name(name)
                    .description(description)
                    .chunkMode(chunkMode)
                    .chunkSize(chunkSize)
                    .chunkOverlap(chunkOverlap)
                    .rerank(rerank)
                    .build();

            KbResponse response = knowledgeBaseService.createKnowledgeBase(request, user);
            String json = toJson(response);
            return successResult(json);
        } catch (Exception e) {
            logger.error("Error creating knowledge base via MCP", e);
            return errorResult("创建知识库失败: " + e.getMessage());
        }
    }

    /**
     * 处理更新知识库（认证参数由 MCP 客户端传入）
     * 支持同时更新名称/描述和 RAG 配置参数
     */
    public McpSchema.CallToolResult handleKbUpdate(Long kbId, String name, String description,
                                                    String chunkMode, Integer chunkSize,
                                                    Integer chunkOverlap, Boolean rerank,
                                                    String username, String password) {
        logger.info("MCP kb update: kbId={}, name={}, username={}", kbId, name, username);
        try {
            LoginRequest loginRequest = LoginRequest.builder()
                    .username(username)
                    .password(password)
                    .build();
            LoginResponse loginResult = userService.login(loginRequest);
            User user = User.builder()
                    .id(loginResult.getUser().getId())
                    .username(loginResult.getUser().getUsername())
                    .role(loginResult.getUser().getRole() != null ? Role.valueOf(loginResult.getUser().getRole()) : Role.USER)
                    .build();

            KbResponse response = null;

            // 更新名称/描述
            boolean hasBasicUpdate = (name != null && !name.isBlank()) || (description != null && !description.isBlank());
            if (hasBasicUpdate) {
                KbUpdateRequest updateRequest = KbUpdateRequest.builder()
                        .name(name != null && !name.isBlank() ? name : null)
                        .description(description != null && !description.isBlank() ? description : null)
                        .build();
                response = knowledgeBaseService.updateKnowledgeBase(kbId, updateRequest, user);
            }

            // 更新 RAG 配置参数
            boolean hasConfigUpdate = chunkMode != null || chunkSize != null || chunkOverlap != null || rerank != null;
            if (hasConfigUpdate) {
                KbConfigRequest configRequest = KbConfigRequest.builder()
                        .chunkMode(chunkMode)
                        .chunkSize(chunkSize)
                        .chunkOverlap(chunkOverlap)
                        .rerank(rerank)
                        .build();
                knowledgeBaseService.updateConfig(kbId, configRequest, user);
            }

            // 返回最新状态
            if (response == null) {
                response = knowledgeBaseService.getKnowledgeBase(kbId);
            }

            String json = toJson(response);
            return successResult(json);
        } catch (Exception e) {
            logger.error("Error updating knowledge base via MCP", e);
            return errorResult("更新知识库失败: " + e.getMessage());
        }
    }

    /**
     * 处理删除知识库（认证参数由 MCP 客户端传入）
     */
    public McpSchema.CallToolResult handleKbDelete(Long kbId, String username, String password) {
        logger.info("MCP kb delete: kbId={}, username={}", kbId, username);
        try {
            LoginRequest loginRequest = LoginRequest.builder()
                    .username(username)
                    .password(password)
                    .build();
            LoginResponse loginResult = userService.login(loginRequest);
            User user = User.builder()
                    .id(loginResult.getUser().getId())
                    .username(loginResult.getUser().getUsername())
                    .role(loginResult.getUser().getRole() != null ? Role.valueOf(loginResult.getUser().getRole()) : Role.USER)
                    .build();

            knowledgeBaseService.deleteKnowledgeBase(kbId, user);
            String json = toJson(new KbDeleteResponse(kbId, true));
            return successResult(json);
        } catch (Exception e) {
            logger.error("Error deleting knowledge base via MCP", e);
            return errorResult("删除知识库失败: " + e.getMessage());
        }
    }

    /**
     * 处理获取知识库文档上传信息
     */
    public McpSchema.CallToolResult handleKbUploadDocument(Long kbId) {
        logger.info("MCP kb upload document info: kbId={}", kbId);
        try {
            KbResponse kb = knowledgeBaseService.getKnowledgeBase(kbId);
            String json = toJson(new KbUploadDocumentInfoResponse(
                    kbId,
                    kb.getName(),
                    "/api/v1/knowledge/" + kbId + "/documents",
                    "POST",
                    "multipart/form-data",
                    "file (必填, 单个文件)",
                    "50MB per file",
                    "需要 JWT 认证（Authorization: Bearer <token>）"
            ));
            return successResult(json);
        } catch (Exception e) {
            logger.error("Error getting kb upload document info", e);
            return errorResult("获取文档上传信息失败: " + e.getMessage());
        }
    }

    /**
     * 版本号最后一位自动递增
     * "1.0.0" → "1.0.1", "1.0.0-beta" → "1.0.1-beta", "1.0.alpha" → "1.0.alpha.1"
     */
    private String incrementVersion(String currentVersion) {
        if (currentVersion == null || currentVersion.isBlank()) {
            return "1.0.1";
        }
        int lastDot = currentVersion.lastIndexOf('.');
        if (lastDot < 0) {
            return currentVersion + ".1";
        }
        String prefix = currentVersion.substring(0, lastDot + 1);
        String lastSegment = currentVersion.substring(lastDot + 1);

        // 提取最后一段开头的数字部分
        int digitEnd = 0;
        while (digitEnd < lastSegment.length() && Character.isDigit(lastSegment.charAt(digitEnd))) {
            digitEnd++;
        }
        if (digitEnd == 0) {
            // 最后一段不以数字开头（如 "alpha"）
            return currentVersion + ".1";
        }
        String numPart = lastSegment.substring(0, digitEnd);
        String suffixPart = lastSegment.substring(digitEnd);
        int num = Integer.parseInt(numPart);
        return prefix + (num + 1) + suffixPart;
    }

    private McpSchema.CallToolResult successResult(String json) {
        return McpSchema.CallToolResult.builder(List.of(new McpSchema.TextContent(json)))
                .isError(false)
                .build();
    }

    private McpSchema.CallToolResult errorResult(String message) {
        return McpSchema.CallToolResult.builder(List.of(new McpSchema.TextContent(
                        toJson(new ErrorResponse(message)))))
                .isError(true)
                .build();
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
        public String version;
        public String content;
        public String category;
        public ToolDetailResponse(Long id, String name, String version, String content, String category) {
            this.id = id;
            this.name = name;
            this.version = version;
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

    private static class FileUploadInfoResponse {
        public Long toolId;
        public String toolName;
        public String uploadUrl;
        public String httpMethod;
        public String contentType;
        public String formFields;
        public String limits;
        public String instruction;
        public FileUploadInfoResponse(Long toolId, String toolName, String uploadUrl, String httpMethod,
                                       String contentType, String formFields, String limits) {
            this.toolId = toolId;
            this.toolName = toolName;
            this.uploadUrl = uploadUrl;
            this.httpMethod = httpMethod;
            this.contentType = contentType;
            this.formFields = formFields;
            this.limits = limits;
            this.instruction = "使用 HTTP " + httpMethod + " 请求 " + uploadUrl
                    + "，Content-Type 设为 " + contentType
                    + "，表单字段: " + formFields;
        }
    }

    private static class FileDeleteResponse {
        public Long toolId;
        public Long fileId;
        public boolean deleted;
        public FileDeleteResponse(Long toolId, Long fileId, boolean deleted) {
            this.toolId = toolId;
            this.fileId = fileId;
            this.deleted = deleted;
        }
    }

    // ── KB DTO 类 ──────────────────────────────────────────────

    private static class KbListResponse {
        public List<KbResponse> knowledgeBases;
        public long totalElements;
        public int totalPages;
        public int page;
        public int size;
        public KbListResponse(List<KbResponse> knowledgeBases, long totalElements, int totalPages, int page, int size) {
            this.knowledgeBases = knowledgeBases;
            this.totalElements = totalElements;
            this.totalPages = totalPages;
            this.page = page;
            this.size = size;
        }
    }

    private static class KbSearchResponse {
        public List<KbSearchResultResponse> results;
        public int count;
        public KbSearchResponse(List<KbSearchResultResponse> results) {
            this.results = results;
            this.count = results.size();
        }
    }

    private static class KbDeleteResponse {
        public Long kbId;
        public boolean deleted;
        public KbDeleteResponse(Long kbId, boolean deleted) {
            this.kbId = kbId;
            this.deleted = deleted;
        }
    }

    private static class KbUploadDocumentInfoResponse {
        public Long kbId;
        public String kbName;
        public String uploadUrl;
        public String httpMethod;
        public String contentType;
        public String formFields;
        public String limits;
        public String requiresAuth;
        public String instruction;
        public KbUploadDocumentInfoResponse(Long kbId, String kbName, String uploadUrl, String httpMethod,
                                            String contentType, String formFields, String limits, String requiresAuth) {
            this.kbId = kbId;
            this.kbName = kbName;
            this.uploadUrl = uploadUrl;
            this.httpMethod = httpMethod;
            this.contentType = contentType;
            this.formFields = formFields;
            this.limits = limits;
            this.requiresAuth = requiresAuth;
            this.instruction = "使用 HTTP " + httpMethod + " 请求 " + uploadUrl
                    + "，Content-Type 设为 " + contentType
                    + "，表单字段: " + formFields
                    + "。" + requiresAuth;
        }
    }
}