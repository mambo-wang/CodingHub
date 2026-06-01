package com.iaihub.toolbox.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * MCP Server 配置类 - 使用原生 Java MCP SDK
 *
 * <p>提供以下工具：
 * <ul>
 *   <li>h3_coding_hub_tool_search - 搜索工具列表</li>
 *   <li>h3_coding_hub_tool_get - 获取工具详情</li>
 *   <li>h3_coding_hub_tool_files - 获取工具文件</li>
 *   <li>h3_coding_hub_post_search - 搜索帖子</li>
 *   <li>h3_coding_hub_post_get - 获取帖子详情</li>
 * </ul>
 */
@Configuration
public class McpSdkServerConfig {

    private static final Logger logger = LoggerFactory.getLogger(McpSdkServerConfig.class);

    @Bean
    public HttpServletSseServerTransportProvider servletSseServerTransportProvider(ObjectMapper objectMapper) {
        return new HttpServletSseServerTransportProvider(objectMapper, "/mcp/message");
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
                .serverInfo("H3CodingHub-MCP-Server", "1.0.0")
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
                (exchange, args) -> {
                    String query = args.containsKey("query") ? String.valueOf(args.get("query")) : null;
                    String category = args.containsKey("category") ? String.valueOf(args.get("category")) : null;
                    Integer limit = args.containsKey("limit") ? ((Number) args.get("limit")).intValue() : 20;
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
                (exchange, args) -> {
                    Long toolId = ((Number) args.get("toolId")).longValue();
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
                (exchange, args) -> {
                    Long toolId = ((Number) args.get("toolId")).longValue();
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
                (exchange, args) -> {
                    String query = args.containsKey("query") ? String.valueOf(args.get("query")) : null;
                    Integer limit = args.containsKey("limit") ? ((Number) args.get("limit")).intValue() : 20;
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
                (exchange, args) -> {
                    Long postId = ((Number) args.get("postId")).longValue();
                    return toolHandler.handlePostGet(postId);
                });

        // 注册 h3_coding_hub_tool_download 工具
        registerTool(mcpSyncServer, "h3_coding_hub_tool_download", "获取工具文件的下载链接，用于下载附件; 本方法返回的事相对路径，需要用mcp地址（http://mcp_server_ip:8080）拼接为完整的下载链接",
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
                (exchange, args) -> {
                    Long toolId = ((Number) args.get("toolId")).longValue();
                    Long fileId = ((Number) args.get("fileId")).longValue();
                    return toolHandler.handleToolDownload(toolId, fileId);
                });

        logger.info("MCP Server initialized with 6 tools");
        return mcpSyncServer;
    }

    private void registerTool(McpSyncServer server, String name, String description,
                              String inputSchema, BiFunction<McpSyncServerExchange, Map<String, Object>, McpSchema.CallToolResult> handler) {
        McpSchema.Tool tool = new McpSchema.Tool(name, description, inputSchema);
        McpServerFeatures.SyncToolSpecification toolHandler = new McpServerFeatures.SyncToolSpecification(tool, handler);
        server.addTool(toolHandler);
    }
}