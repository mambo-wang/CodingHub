package com.iaihub.toolbox.mcp;

import com.iaihub.toolbox.dto.ToolSearchResult;
import com.iaihub.toolbox.model.Tool;
import com.iaihub.toolbox.service.McpSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MCP 资源处理器
 * 处理工具列表和检索
 */
@Component
public class McpResourceHandler {

    private static final Logger logger = LoggerFactory.getLogger(McpResourceHandler.class);

    private final McpSearchService searchService;

    public McpResourceHandler(McpSearchService searchService) {
        this.searchService = searchService;
    }

    /**
     * 返回工具列表
     */
    public List<Map<String, Object>> listTools() {
        List<ToolSearchResult> tools = searchService.searchTools(null, null, 50);
        List<Map<String, Object>> result = new ArrayList<>();

        for (ToolSearchResult tool : tools) {
            result.add(Map.of(
                    "name", "h3_coding_hub_tool_" + tool.getId(),
                    "description", tool.getName() + " - " + tool.getDescription(),
                    "inputSchema", Map.of(
                            "type", "object",
                            "properties", Map.of(
                                    "toolId", Map.of("type", "integer", "description", "Tool ID")
                            )
                    )
            ));
        }

        return result;
    }

    /**
     * 工具搜索
     */
    public List<ToolSearchResult> searchTools(String query, String category, Integer limit) {
        return searchService.searchTools(query, category, limit);
    }

    /**
     * 获取工具详情
     */
    public String getToolContent(Long toolId) {
        Tool tool = searchService.getToolById(toolId);
        if (tool == null) {
            throw new IllegalArgumentException("Tool not found: " + toolId);
        }
        return tool.getContent() != null ? tool.getContent() : "";
    }
}