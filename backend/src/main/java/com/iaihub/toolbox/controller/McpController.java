package com.iaihub.toolbox.controller;

import io.modelcontextprotocol.server.McpSyncServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * MCP HTTP 端点
 *
 * <p>提供 /mcp/health 健康检查端点。实际 MCP 协议交互（Streamable HTTP）
 * 由 McpSdkServerConfig 中注册的 HttpServletStreamableServerTransportProvider 处理。
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