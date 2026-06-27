package com.iaihub.toolbox.mcp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.jackson2.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.server.transport.HttpServletSseServerTransportProvider;
import io.modelcontextprotocol.server.transport.HttpServletStreamableServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.Map;
import java.util.function.BiFunction;

/**
 * MCP Server 配置类 - 使用原生 Java MCP SDK 2.0.0，同时支持两种传输协议：
 *
 * <ul>
 *   <li><b>Streamable HTTP</b>（/mcp）— MCP 协议 2025-03-26，单一端点同时处理 POST 和 GET</li>
 *   <li><b>SSE</b>（/sse + /mcp/message）— 旧版传输，兼容不支持 streamable-http 的客户端</li>
 * </ul>
 *
 * <p>两个 McpServer 实例各自注册相同的 17 个工具，客户端通过任一传输协议均可调用。
 */
@Configuration
public class McpSdkServerConfig {

    private static final Logger logger = LoggerFactory.getLogger(McpSdkServerConfig.class);

    private final ObjectMapper objectMapper;

    public McpSdkServerConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    // ── 公共 Bean ──────────────────────────────────────────────────

    @Bean
    public McpJsonMapper mcpJsonMapper(ObjectMapper objectMapper) {
        return new JacksonMcpJsonMapper(objectMapper);
    }

    // ── Streamable HTTP 传输 ──────────────────────────────────────

    @Bean
    public HttpServletStreamableServerTransportProvider streamableTransportProvider(McpJsonMapper mcpJsonMapper) {
        return HttpServletStreamableServerTransportProvider.builder()
                .jsonMapper(mcpJsonMapper)
                .mcpEndpoint("/mcp")
                .build();
    }

    @Bean
    public ServletRegistrationBean<HttpServletStreamableServerTransportProvider> streamableServletBean(
            HttpServletStreamableServerTransportProvider transportProvider) {
        return new ServletRegistrationBean<>(transportProvider, "/mcp", "/mcp/*");
    }

    // ── SSE 传输（兼容旧客户端）──────────────────────────────────

    @Bean
    public HttpServletSseServerTransportProvider sseTransportProvider(McpJsonMapper mcpJsonMapper) {
        return HttpServletSseServerTransportProvider.builder()
                .jsonMapper(mcpJsonMapper)
                .messageEndpoint("/sse/message")
                .sseEndpoint("/sse")
                .build();
    }

    @Bean
    public ServletRegistrationBean<HttpServletSseServerTransportProvider> sseServletBean(
            HttpServletSseServerTransportProvider transportProvider) {
        return new ServletRegistrationBean<>(transportProvider, "/sse", "/sse/message");
    }

    // ── McpServer 实例 ────────────────────────────────────────────

    /**
     * Streamable HTTP McpServer（主实例，注入到其他组件时使用此实例）。
     */
    @Primary
    @Bean(destroyMethod = "close")
    public McpSyncServer streamableMcpServer(HttpServletStreamableServerTransportProvider transportProvider,
                                             IaihubToolHandler toolHandler) {
        McpSyncServer server = McpServer.sync(transportProvider)
                .serverInfo("H3CodingHub-MCP-Server", "2.0.0")
                .capabilities(McpSchema.ServerCapabilities.builder()
                        .tools(true)
                        .logging()
                        .build())
                .build();

        registerAllTools(server, toolHandler);
        logger.info("MCP Server (streamable-http, /mcp) initialized with 17 tools");
        return server;
    }

    /**
     * SSE McpServer（兼容旧客户端）。
     */
    @Bean(destroyMethod = "close")
    public McpSyncServer sseMcpServer(HttpServletSseServerTransportProvider transportProvider,
                                      IaihubToolHandler toolHandler) {
        McpSyncServer server = McpServer.sync(transportProvider)
                .serverInfo("H3CodingHub-MCP-Server", "2.0.0")
                .capabilities(McpSchema.ServerCapabilities.builder()
                        .tools(true)
                        .logging()
                        .build())
                .build();

        registerAllTools(server, toolHandler);
        logger.info("MCP Server (SSE, /sse) initialized with 17 tools");
        return server;
    }

    // ── 工具注册 ──────────────────────────────────────────────────

    /**
     * 在所有 McpServer 实例上注册相同的 17 个工具。
     */
    private void registerAllTools(McpSyncServer server, IaihubToolHandler toolHandler) {

        registerTool(server, "h3_coding_hub_tool_search", "搜索工具列表，可按关键词和分类搜索",
                """
                {
                    "type":"object",
                    "properties":{
                        "query":{"type":"string","description":"搜索关键词"},
                        "category":{"type":"string","description":"分类名称"},
                        "limit":{"type":"integer","description":"返回数量限制，默认20"}
                    }
                }
                """,
                (exchange, request) -> {
                    Map<String, Object> args = request.arguments();
                    String query = args != null && args.containsKey("query") ? String.valueOf(args.get("query")) : null;
                    String category = args != null && args.containsKey("category") ? String.valueOf(args.get("category")) : null;
                    Integer limit = args != null && args.containsKey("limit") ? ((Number) args.get("limit")).intValue() : 20;
                    return toolHandler.handleToolSearch(query, category, limit);
                });

        registerTool(server, "h3_coding_hub_tool_get", "获取工具详情，包括完整的 markdown 文档",
                """
                {
                    "type":"object",
                    "properties":{
                        "toolId":{"type":"integer","description":"工具ID"}
                    },
                    "required":["toolId"]
                }
                """,
                (exchange, request) -> {
                    Long toolId = ((Number) request.arguments().get("toolId")).longValue();
                    return toolHandler.handleToolGet(toolId);
                });

        registerTool(server, "h3_coding_hub_tool_files", "获取工具文件下载信息",
                """
                {
                    "type":"object",
                    "properties":{
                        "toolId":{"type":"integer","description":"工具ID"}
                    },
                    "required":["toolId"]
                }
                """,
                (exchange, request) -> {
                    Long toolId = ((Number) request.arguments().get("toolId")).longValue();
                    return toolHandler.handleToolFiles(toolId);
                });

        registerTool(server, "h3_coding_hub_post_search", "搜索社区帖子",
                """
                {
                    "type":"object",
                    "properties":{
                        "query":{"type":"string","description":"搜索关键词"},
                        "limit":{"type":"integer","description":"返回数量限制，默认20"}
                    }
                }
                """,
                (exchange, request) -> {
                    Map<String, Object> args = request.arguments();
                    String query = args != null && args.containsKey("query") ? String.valueOf(args.get("query")) : null;
                    Integer limit = args != null && args.containsKey("limit") ? ((Number) args.get("limit")).intValue() : 20;
                    return toolHandler.handlePostSearch(query, limit);
                });

        registerTool(server, "h3_coding_hub_post_get", "获取帖子内容，包括完整的 markdown",
                """
                {
                    "type":"object",
                    "properties":{
                        "postId":{"type":"integer","description":"帖子ID"}
                    },
                    "required":["postId"]
                }
                """,
                (exchange, request) -> {
                    Long postId = ((Number) request.arguments().get("postId")).longValue();
                    return toolHandler.handlePostGet(postId);
                });

        registerTool(server, "h3_coding_hub_tool_download", "获取工具文件的下载链接，用于下载附件; 本方法返回的是相对路径，需要用mcp地址（http://mcp_server_ip:8082）拼接为完整的下载链接",
                """
                {
                    "type":"object",
                    "properties":{
                        "toolId":{"type":"integer","description":"工具ID"},
                        "fileId":{"type":"integer","description":"文件ID"}
                    },
                    "required":["toolId","fileId"]
                }
                """,
                (exchange, request) -> {
                    Long toolId = ((Number) request.arguments().get("toolId")).longValue();
                    Long fileId = ((Number) request.arguments().get("fileId")).longValue();
                    return toolHandler.handleToolDownload(toolId, fileId);
                });

        registerTool(server, "h3_coding_hub_tool_create", """
                创建新工具。需要传入账号密码进行认证，MCP客户端应传入客户端所在系统的登录账号，密码默认为123456。
                创建成功后返回工具ID，可使用该ID通过 h3_coding_hub_tool_file_upload 工具上传文件到该工具下。
                """,
                """
                {
                    "type":"object",
                    "properties":{
                        "name":{"type":"string","description":"工具名称"},
                        "categoryId":{"type":"integer","description":"分类ID"},
                        "content":{"type":"string","description":"工具内容/文档"},
                        "version":{"type":"string","description":"版本号，如1.0.0"},
                        "username":{"type":"string","description":"登录账号，MCP客户端应传入客户端所在系统的登录账号"},
                        "password":{"type":"string","description":"登录密码，默认123456"}
                    },
                    "required":["name","categoryId","content","version","username","password"]
                }
                """,
                (exchange, request) -> {
                    Map<String, Object> args = request.arguments();
                    String name = String.valueOf(args.get("name"));
                    Long categoryId = ((Number) args.get("categoryId")).longValue();
                    String content = String.valueOf(args.get("content"));
                    String version = String.valueOf(args.get("version"));
                    String username = String.valueOf(args.get("username"));
                    String password = String.valueOf(args.get("password"));
                    return toolHandler.handleToolCreate(name, categoryId, content, version, username, password);
                });

        registerTool(server, "h3_coding_hub_post_create", "创建新帖子。需要传入账号密码进行认证，MCP客户端应传入客户端所在系统的登录账号，密码默认为123456",
                """
                {
                    "type":"object",
                    "properties":{
                        "title":{"type":"string","description":"帖子标题"},
                        "content":{"type":"string","description":"帖子内容"},
                        "categoryId":{"type":"integer","description":"帖子分类ID"},
                        "username":{"type":"string","description":"登录账号，MCP客户端应传入客户端所在系统的登录账号"},
                        "password":{"type":"string","description":"登录密码，默认123456"}
                    },
                    "required":["title","content","categoryId","username","password"]
                }
                """,
                (exchange, request) -> {
                    Map<String, Object> args = request.arguments();
                    String title = String.valueOf(args.get("title"));
                    String content = String.valueOf(args.get("content"));
                    Long categoryId = ((Number) args.get("categoryId")).longValue();
                    String username = String.valueOf(args.get("username"));
                    String password = String.valueOf(args.get("password"));
                    return toolHandler.handlePostCreate(title, content, categoryId, username, password);
                });

        registerTool(server, "h3_coding_hub_tool_file_upload", """
                上传文件到指定工具。本工具告知客户端文件上传的 REST API 接口信息。
                客户端应使用 HTTP Multipart POST 请求直接上传文件，无需认证（已放通权限）。
                
                REST API 详情：
                - URL: POST {mcp_server_base_url}/api/v1/tools/{toolId}/files
                - Content-Type: multipart/form-data
                - 表单字段:
                  - files: 文件列表（必填，可多个），支持任意文件类型
                  - readme: README 内容（可选，markdown 文本）
                - 限制: 单文件最大 50MB，总上传大小最大 200MB
                - 响应: JSON，包含 toolId、files 列表、readmeSaved 字段
                
                使用步骤：
                1. 先调用 h3_coding_hub_tool_create 创建工具，获取 toolId
                2. 使用本工具获取上传接口信息
                3. 客户端通过 HTTP Multipart POST 上传文件到对应 toolId
                """,
                """
                {
                    "type":"object",
                    "properties":{
                        "toolId":{"type":"integer","description":"工具ID（先通过 h3_coding_hub_tool_create 创建获取）"}
                    },
                    "required":["toolId"]
                }
                """,
                (exchange, request) -> {
                    Long toolId = ((Number) request.arguments().get("toolId")).longValue();
                    return toolHandler.handleToolFileUploadInfo(toolId);
                });

        registerTool(server, "h3_coding_hub_tool_modify", """
                修改已创建的工具。需要传入账号密码进行认证，MCP客户端应传入客户端所在系统的登录账号，密码默认为123456。
                版本号（version）可以不传，系统会自动在当前版本号最后一位+1（如1.0.0→1.0.1）。
                只会更新传入的字段，未传入的字段保持不变。
                """,
                """
                {
                    "type":"object",
                    "properties":{
                        "toolId":{"type":"integer","description":"要修改的工具ID"},
                        "name":{"type":"string","description":"新的工具名称"},
                        "categoryId":{"type":"integer","description":"新的分类ID"},
                        "content":{"type":"string","description":"新的工具描述/文档"},
                        "version":{"type":"string","description":"版本号，不传则自动递增最后一位"},
                        "username":{"type":"string","description":"登录账号，MCP客户端应传入客户端所在系统的登录账号"},
                        "password":{"type":"string","description":"登录密码，默认123456"}
                    },
                    "required":["toolId","username","password"]
                }
                """,
                (exchange, request) -> {
                    Map<String, Object> args = request.arguments();
                    Long toolId = ((Number) args.get("toolId")).longValue();
                    String name = args.containsKey("name") ? String.valueOf(args.get("name")) : null;
                    Long categoryId = args.containsKey("categoryId") ? ((Number) args.get("categoryId")).longValue() : null;
                    String content = args.containsKey("content") ? String.valueOf(args.get("content")) : null;
                    String version = args.containsKey("version") ? String.valueOf(args.get("version")) : null;
                    String username = String.valueOf(args.get("username"));
                    String password = String.valueOf(args.get("password"));
                    return toolHandler.handleToolModify(toolId, name, categoryId, content, version, username, password);
                });

        registerTool(server, "h3_coding_hub_tool_file_delete", """
                删除指定工具下的指定文件。需要传入账号密码进行认证，MCP客户端应传入客户端所在系统的登录账号，密码默认为123456。
                只能删除自己创建的工具下的文件。删除时会同时移除物理文件和数据库记录。
                """,
                """
                {
                    "type":"object",
                    "properties":{
                        "toolId":{"type":"integer","description":"工具ID"},
                        "fileId":{"type":"integer","description":"要删除的文件ID"},
                        "username":{"type":"string","description":"登录账号，MCP客户端应传入客户端所在系统的登录账号"},
                        "password":{"type":"string","description":"登录密码，默认123456"}
                    },
                    "required":["toolId","fileId","username","password"]
                }
                """,
                (exchange, request) -> {
                    Map<String, Object> args = request.arguments();
                    Long toolId = ((Number) args.get("toolId")).longValue();
                    Long fileId = ((Number) args.get("fileId")).longValue();
                    String username = String.valueOf(args.get("username"));
                    String password = String.valueOf(args.get("password"));
                    return toolHandler.handleToolFileDelete(toolId, fileId, username, password);
                });

        // ── 知识库 MCP 工具 ──────────────────────────────────────

        registerTool(server, "h3_coding_hub_kb_list", "获取知识库列表，支持分页和排序",
                """
                {
                    "type":"object",
                    "properties":{
                        "page":{"type":"integer","description":"页码，从0开始，默认0"},
                        "size":{"type":"integer","description":"每页条数，默认20"},
                        "sortBy":{"type":"string","description":"排序方式，可选 'hot'（按热度）或留空（按最新）"}
                    }
                }
                """,
                (exchange, request) -> {
                    Map<String, Object> args = request.arguments();
                    Integer page = args != null && args.containsKey("page") ? ((Number) args.get("page")).intValue() : 0;
                    Integer size = args != null && args.containsKey("size") ? ((Number) args.get("size")).intValue() : 20;
                    String sortBy = args != null && args.containsKey("sortBy") ? String.valueOf(args.get("sortBy")) : null;
                    return toolHandler.handleKbList(page, size, sortBy);
                });

        registerTool(server, "h3_coding_hub_kb_search", "对指定知识库执行语义搜索，返回相关片段",
                """
                {
                    "type":"object",
                    "properties":{
                        "kbId":{"type":"integer","description":"知识库ID"},
                        "query":{"type":"string","description":"搜索关键词"},
                        "topK":{"type":"integer","description":"返回结果数量，默认5"},
                        "rerank":{"type":"boolean","description":"是否启用重排序"},
                        "expandContext":{"type":"integer","description":"上下文扩展块数，默认0"}
                    },
                    "required":["kbId","query"]
                }
                """,
                (exchange, request) -> {
                    Map<String, Object> args = request.arguments();
                    Long kbId = ((Number) args.get("kbId")).longValue();
                    String query = String.valueOf(args.get("query"));
                    Integer topK = args.containsKey("topK") ? ((Number) args.get("topK")).intValue() : null;
                    Boolean rerank = args.containsKey("rerank") ? (Boolean) args.get("rerank") : null;
                    Integer expandContext = args.containsKey("expandContext") ? ((Number) args.get("expandContext")).intValue() : null;
                    return toolHandler.handleKbSearch(kbId, query, topK, rerank, expandContext);
                });

        registerTool(server, "h3_coding_hub_kb_create", """
                创建新知识库。需要传入账号密码进行认证，MCP客户端应传入客户端所在系统的登录账号，密码默认为123456。
                创建成功后返回知识库ID，可使用该ID通过 h3_coding_hub_kb_upload_document 工具上传文档。
                """,
                """
                {
                    "type":"object",
                    "properties":{
                        "name":{"type":"string","description":"知识库名称"},
                        "description":{"type":"string","description":"知识库描述"},
                        "chunkMode":{"type":"string","description":"分块模式，默认'structural'"},
                        "chunkSize":{"type":"integer","description":"分块大小，默认800"},
                        "chunkOverlap":{"type":"integer","description":"分块重叠，默认50"},
                        "rerank":{"type":"boolean","description":"是否启用重排序，默认true"},
                        "username":{"type":"string","description":"登录账号，MCP客户端应传入客户端所在系统的登录账号"},
                        "password":{"type":"string","description":"登录密码，默认123456"}
                    },
                    "required":["name","username","password"]
                }
                """,
                (exchange, request) -> {
                    Map<String, Object> args = request.arguments();
                    String name = String.valueOf(args.get("name"));
                    String description = args.containsKey("description") ? String.valueOf(args.get("description")) : null;
                    String chunkMode = args.containsKey("chunkMode") ? String.valueOf(args.get("chunkMode")) : null;
                    Integer chunkSize = args.containsKey("chunkSize") ? ((Number) args.get("chunkSize")).intValue() : null;
                    Integer chunkOverlap = args.containsKey("chunkOverlap") ? ((Number) args.get("chunkOverlap")).intValue() : null;
                    Boolean rerank = args.containsKey("rerank") ? (Boolean) args.get("rerank") : null;
                    String username = String.valueOf(args.get("username"));
                    String password = String.valueOf(args.get("password"));
                    return toolHandler.handleKbCreate(name, description, chunkMode, chunkSize, chunkOverlap, rerank, username, password);
                });

        registerTool(server, "h3_coding_hub_kb_update", """
                更新知识库，支持修改名称、描述和 RAG 配置参数（分块模式、分块大小等）。
                需要传入账号密码进行认证，MCP客户端应传入客户端所在系统的登录账号，密码默认为123456。
                只更新传入的字段，未传入的字段保持不变。
                """,
                """
                {
                    "type":"object",
                    "properties":{
                        "kbId":{"type":"integer","description":"知识库ID"},
                        "name":{"type":"string","description":"新的知识库名称"},
                        "description":{"type":"string","description":"新的知识库描述"},
                        "chunkMode":{"type":"string","description":"分块模式，如'structural'"},
                        "chunkSize":{"type":"integer","description":"分块大小"},
                        "chunkOverlap":{"type":"integer","description":"分块重叠"},
                        "rerank":{"type":"boolean","description":"是否启用重排序"},
                        "username":{"type":"string","description":"登录账号，MCP客户端应传入客户端所在系统的登录账号"},
                        "password":{"type":"string","description":"登录密码，默认123456"}
                    },
                    "required":["kbId","username","password"]
                }
                """,
                (exchange, request) -> {
                    Map<String, Object> args = request.arguments();
                    Long kbId = ((Number) args.get("kbId")).longValue();
                    String name = args.containsKey("name") ? String.valueOf(args.get("name")) : null;
                    String description = args.containsKey("description") ? String.valueOf(args.get("description")) : null;
                    String chunkMode = args.containsKey("chunkMode") ? String.valueOf(args.get("chunkMode")) : null;
                    Integer chunkSize = args.containsKey("chunkSize") ? ((Number) args.get("chunkSize")).intValue() : null;
                    Integer chunkOverlap = args.containsKey("chunkOverlap") ? ((Number) args.get("chunkOverlap")).intValue() : null;
                    Boolean rerank = args.containsKey("rerank") ? (Boolean) args.get("rerank") : null;
                    String username = String.valueOf(args.get("username"));
                    String password = String.valueOf(args.get("password"));
                    return toolHandler.handleKbUpdate(kbId, name, description, chunkMode, chunkSize, chunkOverlap, rerank, username, password);
                });

        registerTool(server, "h3_coding_hub_kb_delete", "删除知识库。需要传入账号密码进行认证，MCP客户端应传入客户端所在系统的登录账号，密码默认为123456。",
                """
                {
                    "type":"object",
                    "properties":{
                        "kbId":{"type":"integer","description":"知识库ID"},
                        "username":{"type":"string","description":"登录账号，MCP客户端应传入客户端所在系统的登录账号"},
                        "password":{"type":"string","description":"登录密码，默认123456"}
                    },
                    "required":["kbId","username","password"]
                }
                """,
                (exchange, request) -> {
                    Map<String, Object> args = request.arguments();
                    Long kbId = ((Number) args.get("kbId")).longValue();
                    String username = String.valueOf(args.get("username"));
                    String password = String.valueOf(args.get("password"));
                    return toolHandler.handleKbDelete(kbId, username, password);
                });

        registerTool(server, "h3_coding_hub_kb_upload_document", """
                获取知识库文档上传的 REST API 信息。
                
                MCP 协议不直接支持二进制文件传输。要上传文件到知识库，请使用 REST API。
                本工具返回上传端点 URL、支持的文件类型、认证方式和 curl 示例。
                
                支持的文件类型：md, txt, pdf, docx, pptx, xlsx, py, js, ts, java, go 等
                限制：单文件最大 50MB
                认证：需要 Authorization: Bearer <token> 头（通过 POST /api/v1/auth/login 获取）
                
                工作流程：
                1. 先调用 h3_coding_hub_kb_create 创建知识库，获取 kbId
                2. 调用本工具获取上传接口信息
                3. 通过 HTTP Multipart POST 上传文件（参考返回的 curlExample）
                """,
                """
                {
                    "type":"object",
                    "properties":{
                        "kbId":{"type":"integer","description":"知识库ID"}
                    },
                    "required":["kbId"]
                }
                """,
                (exchange, request) -> {
                    Map<String, Object> args = request.arguments();
                    Long kbId = ((Number) args.get("kbId")).longValue();
                    return toolHandler.handleKbUploadDocument(kbId);
                });
    }

    private void registerTool(McpSyncServer server, String name, String description,
                              String inputSchemaJson,
                              BiFunction<McpSyncServerExchange, McpSchema.CallToolRequest, McpSchema.CallToolResult> handler) {
        try {
            Map<String, Object> inputSchema = objectMapper.readValue(
                    inputSchemaJson, new TypeReference<Map<String, Object>>() {});
            McpSchema.Tool tool = McpSchema.Tool.builder(name, inputSchema)
                    .description(description)
                    .build();
            McpServerFeatures.SyncToolSpecification toolHandler =
                    McpServerFeatures.SyncToolSpecification.builder()
                            .tool(tool)
                            .callHandler(handler)
                            .build();
            server.addTool(toolHandler);
        } catch (Exception e) {
            logger.error("Failed to register tool: {}", name, e);
            throw new RuntimeException("Failed to register tool: " + name, e);
        }
    }
}
