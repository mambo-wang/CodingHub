package com.iaihub.toolbox.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iaihub.toolbox.dto.ToolSearchResult;
import com.iaihub.toolbox.model.Tool;
import com.iaihub.toolbox.service.McpSearchService;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * MCP 资源处理器 — 将工具广场数据暴露为标准 MCP Resource
 *
 * <p>提供以下资源：
 * <ul>
 *   <li>{@code codinghub://tools/catalog} — 工具广场目录（全量工具摘要）</li>
 *   <li>{@code codinghub://tools/recent} — 最近更新的工具（近 7 天）</li>
 *   <li>{@code codinghub://tool/{id}} — 单个工具详情（Resource Template）</li>
 * </ul>
 *
 * <p>当工具新增、更新、删除时，由 {@link McpNotificationService} 发送
 * {@code notifications/resources/updated} 和 {@code notifications/resources/list_changed}
 * 通知已连接的 MCP 客户端。
 */
@Component
public class McpResourceHandler {

    private static final Logger logger = LoggerFactory.getLogger(McpResourceHandler.class);

    public static final String CATALOG_URI = "codinghub://tools/catalog";
    public static final String RECENT_URI = "codinghub://tools/recent";
    public static final String TOOL_URI_TEMPLATE = "codinghub://tool/{id}";

    private final McpSearchService searchService;
    private final ObjectMapper objectMapper;

    public McpResourceHandler(McpSearchService searchService, ObjectMapper objectMapper) {
        this.searchService = searchService;
        this.objectMapper = objectMapper;
    }

    // ── 静态资源读取 ─────────────────────────────────────────────

    /**
     * 读取工具广场目录 — 所有正常状态的工具摘要。
     */
    public McpSchema.ReadResourceResult readCatalog() {
        try {
            List<ToolSearchResult> tools = searchService.searchTools(null, null, null, 200);
            String json = objectMapper.writeValueAsString(
                    Map.of("tools", tools, "count", tools.size()));
            return textResult(CATALOG_URI, json);
        } catch (Exception e) {
            logger.error("Failed to read tool catalog resource", e);
            return errorResult(CATALOG_URI, "Failed to load catalog: " + e.getMessage());
        }
    }

    /**
     * 读取最近更新的工具 — 由 {@code searchTools} 按默认排序返回前 20 条。
     */
    public McpSchema.ReadResourceResult readRecent() {
        try {
            List<ToolSearchResult> tools = searchService.searchTools(null, null, null, 20);
            String json = objectMapper.writeValueAsString(
                    Map.of("recentTools", tools, "count", tools.size()));
            return textResult(RECENT_URI, json);
        } catch (Exception e) {
            logger.error("Failed to read recent tools resource", e);
            return errorResult(RECENT_URI, "Failed to load recent tools: " + e.getMessage());
        }
    }

    // ── Resource Template 读取 ────────────────────────────────────

    /**
     * 读取单个工具详情 — 从 URI 中解析工具 ID 并返回完整信息。
     */
    public McpSchema.ReadResourceResult readTool(McpSchema.ReadResourceRequest request) {
        String uri = request.uri();
        try {
            String idStr = uri.replace("codinghub://tool/", "");
            Long toolId = Long.parseLong(idStr);

            Tool tool = searchService.getToolById(toolId);
            if (tool == null) {
                return errorResult(uri, "Tool not found: " + toolId);
            }

            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("id", tool.getId());
            detail.put("name", tool.getName());
            detail.put("version", tool.getVersion() != null ? tool.getVersion() : "1.0.0");
            detail.put("content", tool.getContent() != null ? tool.getContent() : "");
            detail.put("category", tool.getCategory() != null ? tool.getCategory().getName() : "");
            detail.put("viewCount", tool.getViewCount());
            detail.put("likeCount", tool.getLikeCount());
            detail.put("score", tool.getScore());

            String json = objectMapper.writeValueAsString(detail);
            return textResult(uri, json);
        } catch (NumberFormatException e) {
            return errorResult(uri, "Invalid tool ID in URI: " + uri);
        } catch (Exception e) {
            logger.error("Failed to read tool resource: {}", uri, e);
            return errorResult(uri, "Failed to load tool: " + e.getMessage());
        }
    }

    // ── 辅助方法 ──────────────────────────────────────────────────

    private McpSchema.ReadResourceResult textResult(String uri, String json) {
        return new McpSchema.ReadResourceResult(List.of(
                new McpSchema.TextResourceContents(uri, "application/json", json)));
    }

    private McpSchema.ReadResourceResult errorResult(String uri, String message) {
        String json = "{\"error\":\"" + message.replace("\"", "\\\"") + "\"}";
        return new McpSchema.ReadResourceResult(List.of(
                new McpSchema.TextResourceContents(uri, "application/json", json)));
    }
}
