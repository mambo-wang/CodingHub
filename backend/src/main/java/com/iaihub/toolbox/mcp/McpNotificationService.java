package com.iaihub.toolbox.mcp;

import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * MCP 资源变更通知服务
 *
 * <p>当工具广场的工具发生新增、更新、删除时，向所有已连接的 MCP 客户端发送通知：
 * <ul>
 *   <li>{@code notifications/resources/list_changed} — 工具列表整体有变化</li>
 *   <li>{@code notifications/resources/updated} — 指定 URI 的资源内容已更新（携带 URI）</li>
 * </ul>
 *
 * <p>当前主流 MCP 客户端（CodeBuddy、QoderWork）对这些通知的 UI 支持仍在演进中，
 * 但服务端先行实现，以便客户端跟进后自动生效。
 */
@Service
public class McpNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(McpNotificationService.class);

    private final List<McpSyncServer> mcpServers;

    /**
     * 使用 @Lazy 注入 List&lt;McpSyncServer&gt; 以打破循环依赖：
     * streamableMcpServer → IaihubToolHandler → McpNotificationService → List&lt;McpSyncServer&gt;
     * @Lazy 使 Spring 注入代理对象，在首次调用时才真正解析目标 Bean。
     */
    public McpNotificationService(@Lazy List<McpSyncServer> mcpServers) {
        this.mcpServers = mcpServers;
        logger.info("McpNotificationService initialized with lazy MCP server(s) reference");
    }

    /**
     * 工具新增 — 发送 list_changed + 新工具的 updated 通知。
     */
    public void notifyToolCreated(Long toolId, String toolName) {
        logger.info("MCP notify: tool created — id={}, name={}", toolId, toolName);
        sendListChanged();
        sendResourceUpdated(McpResourceHandler.CATALOG_URI);
        sendResourceUpdated("codinghub://tool/" + toolId);
    }

    /**
     * 工具更新 — 发送 list_changed + 该工具 + catalog 的 updated 通知。
     */
    public void notifyToolUpdated(Long toolId, String toolName) {
        logger.info("MCP notify: tool updated — id={}, name={}", toolId, toolName);
        sendListChanged();
        sendResourceUpdated(McpResourceHandler.CATALOG_URI);
        sendResourceUpdated("codinghub://tool/" + toolId);
    }

    /**
     * 工具删除 — 发送 list_changed + catalog 的 updated 通知。
     */
    public void notifyToolDeleted(Long toolId) {
        logger.info("MCP notify: tool deleted — id={}", toolId);
        sendListChanged();
        sendResourceUpdated(McpResourceHandler.CATALOG_URI);
    }

    // ── 内部方法 ──────────────────────────────────────────────────

    private void sendListChanged() {
        for (McpSyncServer server : mcpServers) {
            try {
                server.notifyResourcesListChanged();
            } catch (Exception e) {
                logger.warn("Failed to send resources/list_changed notification: {}", e.getMessage());
            }
        }
    }

    private void sendResourceUpdated(String uri) {
        McpSchema.ResourcesUpdatedNotification notification =
                new McpSchema.ResourcesUpdatedNotification(uri);
        for (McpSyncServer server : mcpServers) {
            try {
                server.notifyResourcesUpdated(notification);
            } catch (Exception e) {
                logger.warn("Failed to send resources/updated notification for {}: {}", uri, e.getMessage());
            }
        }
    }
}
