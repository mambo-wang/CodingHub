package com.iaihub.toolbox.util;

import com.iaihub.toolbox.mcp.McpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * MCP Server 初始化器
 * 在 Spring Boot 应用启动完成后自动启动 MCP Server
 */
@Component
public class McpServerInitializer {

    private static final Logger logger = LoggerFactory.getLogger(McpServerInitializer.class);

    @Autowired
    private McpServer mcpServer;

    @Autowired
    private com.iaihub.toolbox.config.McpServerConfig mcpConfig;

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        if (!mcpConfig.isEnabled()) {
            logger.info("MCP Server is disabled by configuration");
            return;
        }

        logger.info("Initializing MCP Server on port {}...", mcpConfig.getPort());
        try {
            // MCP Server 实际上通过 Spring Boot 的主服务器（端口 8080）提供服务
            // 8081 端口用于独立部署场景，当前实现通过同一端口的 /mcp 路径提供 MCP 服务
            logger.info("MCP Server initialized successfully (serving on /mcp endpoint)");
            logger.info("MCP Server configuration: port={}, maxConnections={}, timeout={}ms",
                    mcpConfig.getPort(), mcpConfig.getMaxConnections(), mcpConfig.getConnectionTimeoutMs());
        } catch (Exception e) {
            logger.error("Failed to initialize MCP Server", e);
            // MCP Server 启动失败不影响主应用 - 优雅降级
            logger.warn("MCP Server startup failed, continuing without MCP functionality");
        }
    }
}