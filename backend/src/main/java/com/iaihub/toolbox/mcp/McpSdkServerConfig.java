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
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.function.BiFunction;

/**
 * MCP Server 配置类 - 使用原生 Java MCP SDK 2.0.0
 *
 * <p>提供以下工具：
 * <ul>
 *   <li>h3_coding_hub_tool_search - 搜索工具列表</li>
 *   <li>h3_coding_hub_tool_get - 获取工具详情</li>
 *   <li>h3_coding_hub_tool_files - 获取工具文件</li>
 *   <li>h3_coding_hub_post_search - 搜索帖子</li>
 *   <li>h3_coding_hub_post_get - 获取帖子详情</li>
 *   <li>h3_coding_hub_tool_download - 获取工具文件下载链接</li>
 *   <li>h3_coding_hub_tool_create - 创建工具（需要认证）</li>
 *   <li>h3_coding_hub_post_create - 创建帖子（需要认证）</li>
 *   <li>h3_coding_hub_tool_file_upload - 获取文件上传接口信息</li>
 * </ul>
 */
@Configuration
public class McpSdkServerConfig {

    private static final Logger logger = LoggerFactory.getLogger(McpSdkServerConfig.class);

    private final ObjectMapper objectMapper;

    public McpSdkServerConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Bean
    public McpJsonMapper mcpJsonMapper(ObjectMapper objectMapper) {
        return new JacksonMcpJsonMapper(objectMapper);
    }

    @Bean
    public HttpServletSseServerTransportProvider servletSseServerTransportProvider(McpJsonMapper mcpJsonMapper) {
        return HttpServletSseServerTransportProvider.builder()
                .jsonMapper(mcpJsonMapper)
                .messageEndpoint("/mcp/message")
                .build();
    }

    @Bean
    public ServletRegistrationBean<HttpServletSseServerTransportProvider> customServletBean(
            HttpServletSseServerTransportProvider transportProvider) {
        return new ServletRegistrationBean<>(transportProvider, "/sse", "/mcp/message");
    }

    @Bean(destroyMethod = "close")
    public McpSyncServer mcpSyncServer(HttpServletSseServerTransportProvider transportProvider,
                                       IaihubToolHandler toolHandler) {
        McpSyncServer mcpSyncServer = McpServer.sync(transportProvider)
                .serverInfo("H3CodingHub-MCP-Server", "2.0.0")
                .capabilities(McpSchema.ServerCapabilities.builder()
                        .tools(true)
                        .logging()
                        .build())
                .build();

        // 注册 h3_coding_hub_tool_search 工具
        registerTool(mcpSyncServer, "h3_coding_hub_tool_search", "搜索工具列表，可按关键词和分类搜索",
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

        // 注册 h3_coding_hub_tool_get 工具
        registerTool(mcpSyncServer, "h3_coding_hub_tool_get", "获取工具详情，包括完整的 markdown 文档",
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

        // 注册 h3_coding_hub_tool_files 工具
        registerTool(mcpSyncServer, "h3_coding_hub_tool_files", "获取工具文件下载信息",
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

        // 注册 h3_coding_hub_post_search 工具
        registerTool(mcpSyncServer, "h3_coding_hub_post_search", "搜索社区帖子",
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

        // 注册 h3_coding_hub_post_get 工具
        registerTool(mcpSyncServer, "h3_coding_hub_post_get", "获取帖子内容，包括完整的 markdown",
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

        // 注册 h3_coding_hub_tool_download 工具
        registerTool(mcpSyncServer, "h3_coding_hub_tool_download", "获取工具文件的下载链接，用于下载附件; 本方法返回的是相对路径，需要用mcp地址（http://mcp_server_ip:8081）拼接为完整的下载链接",
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

        // 注册 h3_coding_hub_tool_create 工具（需要认证）
        registerTool(mcpSyncServer, "h3_coding_hub_tool_create", """
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

        // 注册 h3_coding_hub_post_create 工具（需要认证）
        registerTool(mcpSyncServer, "h3_coding_hub_post_create", "创建新帖子。需要传入账号密码进行认证，MCP客户端应传入客户端所在系统的登录账号，密码默认为123456",
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

        // 注册 h3_coding_hub_tool_file_upload 工具
        registerTool(mcpSyncServer, "h3_coding_hub_tool_file_upload", """
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

        logger.info("MCP Server initialized with 9 tools");
        return mcpSyncServer;
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