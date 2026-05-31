package com.iaihub.toolbox.controller;

import com.iaihub.toolbox.mcp.McpConnectionManager;
import com.iaihub.toolbox.mcp.McpServer;
import com.iaihub.toolbox.mcp.protocol.McpMessage;
import com.iaihub.toolbox.mcp.protocol.McpResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

/**
 * MCP HTTP 端点
 * 提供 POST /mcp, GET /mcp/sse, GET /mcp/health
 */
@RestController
@RequestMapping("/mcp")
public class McpController {

    private static final Logger logger = LoggerFactory.getLogger(McpController.class);

    private final McpServer mcpServer;
    private final McpConnectionManager connectionManager;
    private final ObjectMapper objectMapper;

    public McpController(McpServer mcpServer, McpConnectionManager connectionManager) {
        this.mcpServer = mcpServer;
        this.connectionManager = connectionManager;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * MCP 消息接收端点
     */
    @PostMapping
    public McpResponse handleMcpMessage(@RequestBody McpMessage message) {
        logger.info("Received MCP message: method={}, id={}", message.getMethod(), message.getId());
        return mcpServer.handleMessage(message);
    }

    /**
     * SSE 事件流端点
     */
    @GetMapping(value = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamSse() {
        logger.info("SSE connection requested");
        McpConnectionManager.SseEmitter emitter = new McpConnectionManager.SseEmitter(30 * 60 * 1000L);
        return connectionManager.registerEmitter(emitter).getDelegate();
    }

    /**
     * 健康检查端点
     */
    @GetMapping("/health")
    public Map<String, Object> healthCheck() {
        return Map.of(
                "status", "ok",
                "version", "1.0.0",
                "timestamp", java.time.Instant.now().toString(),
                "activeConnections", mcpServer.getActiveConnectionCount()
        );
    }
}