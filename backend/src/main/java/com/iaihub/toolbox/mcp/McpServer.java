package com.iaihub.toolbox.mcp;

import com.iaihub.toolbox.mcp.protocol.McpMessage;
import com.iaihub.toolbox.mcp.protocol.McpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * MCP Server 核心类
 * 管理连接和消息处理
 */
@Component
public class McpServer {

    private static final Logger logger = LoggerFactory.getLogger(McpServer.class);

    private final McpConnectionManager connectionManager;
    private final McpResourceHandler resourceHandler;
    private final McpToolHandler toolHandler;

    private final AtomicInteger nextId = new AtomicInteger(1);

    public McpServer(McpConnectionManager connectionManager,
                     McpResourceHandler resourceHandler,
                     McpToolHandler toolHandler) {
        this.connectionManager = connectionManager;
        this.resourceHandler = resourceHandler;
        this.toolHandler = toolHandler;
    }

    /**
     * 处理 MCP 消息
     */
    public McpResponse handleMessage(McpMessage message) {
        if (message == null) {
            return McpResponse.error(null, com.iaihub.toolbox.mcp.protocol.McpError.invalidRequest("Message is null"));
        }

        logger.info("Handling MCP message: method={}, id={}", message.getMethod(), message.getId());

        try {
            // 处理工具列表请求
            if (message.isToolsList()) {
                return handleToolsList(message.getId());
            }

            // 处理工具调用请求
            if (message.isToolsCall()) {
                return handleToolsCall(message, message.getId());
            }

            // 未知方法
            return McpResponse.error(message.getId(),
                    com.iaihub.toolbox.mcp.protocol.McpError.methodNotFound(message.getMethod()));

        } catch (Exception e) {
            logger.error("Error handling MCP message", e);
            return McpResponse.error(message.getId(),
                    com.iaihub.toolbox.mcp.protocol.McpError.internalError(e.getMessage()));
        }
    }

    /**
     * 处理工具列表请求
     */
    private McpResponse handleToolsList(Integer id) {
        Map<String, Object> result = Map.of(
                "tools", toolHandler.listTools()
        );
        return McpResponse.success(id, result);
    }

    /**
     * 处理工具调用请求
     */
    private McpResponse handleToolsCall(McpMessage message, Integer id) {
        Map<String, Object> params = message.getParams();
        if (params == null) {
            return McpResponse.error(id,
                    com.iaihub.toolbox.mcp.protocol.McpError.invalidParams("Params required"));
        }

        String toolName = (String) params.get("name");
        @SuppressWarnings("unchecked")
        Map<String, Object> arguments = (Map<String, Object>) params.get("arguments");

        if (toolName == null || toolName.isEmpty()) {
            return McpResponse.error(id,
                    com.iaihub.toolbox.mcp.protocol.McpError.invalidParams("Tool name required"));
        }

        logger.info("Calling tool: name={}, arguments={}", toolName, arguments);

        try {
            Object result = toolHandler.callTool(toolName, arguments);
            return McpResponse.success(id, result);
        } catch (IllegalArgumentException e) {
            return McpResponse.error(id,
                    com.iaihub.toolbox.mcp.protocol.McpError.invalidParams(e.getMessage()));
        } catch (Exception e) {
            logger.error("Error calling tool: {}", toolName, e);
            return McpResponse.error(id,
                    com.iaihub.toolbox.mcp.protocol.McpError.internalError(e.getMessage()));
        }
    }

    /**
     * 生成下一个消息 ID
     */
    public int nextId() {
        return nextId.getAndIncrement();
    }

    /**
     * 获取当前连接数
     */
    public int getActiveConnectionCount() {
        return connectionManager.getActiveConnectionCount();
    }

    /**
     * 关闭服务器
     */
    public void shutdown() {
        logger.info("Shutting down MCP Server");
        connectionManager.shutdown();
    }
}