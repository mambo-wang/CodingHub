package com.iaihub.toolbox.controller;

import io.modelcontextprotocol.server.McpSyncServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * MCP HTTP 端点
 *
 * <p>使用原生 MCP SDK HttpServletSseServerTransportProvider 处理协议交互，
 * 此端点由 ServletRegistrationBean 注册到 /sse 和 /mcp/message。
 *
 * <p>重要：这里的 /sse 端点仅用于健康检查，实际 SSE 连接由 TransportProvider 处理
 */
@RestController
@RequestMapping("/mcp")
public class McpController {

    private static final Logger logger = LoggerFactory.getLogger(McpController.class);

    private final McpSyncServer mcpSyncServer;

    public McpController(McpSyncServer mcpSyncServer) {
        this.mcpSyncServer = mcpSyncServer;
    }

    /**
     * 健康检查端点
     */
    @GetMapping("/health")
    public Map<String, Object> healthCheck() {
        return Map.of(
                "status", "ok",
                "version", "1.0.0",
                "mcpServer", "H3CodingHub-MCP-Server",
                "timestamp", java.time.Instant.now().toString()
        );
    }
}