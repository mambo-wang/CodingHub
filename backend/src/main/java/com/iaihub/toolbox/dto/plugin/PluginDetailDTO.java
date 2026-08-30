package com.iaihub.toolbox.dto.plugin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 插件详情：在摘要基础上增加组件摘要与原始 plugin.json 元数据。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class PluginDetailDTO extends PluginSummaryDTO {

    /** 上传时生成的组件摘要（skills/agents/commands/hooks/.mcp.json/.lsp.json/bin/settings.json）。 */
    private List<ComponentSummary> components;

    /** .codebuddy-plugin/plugin.json 原始内容（解析后）。 */
    private Map<String, Object> pluginJson;

    /** zip 原件文件名（下载用）。 */
    private String sourceZipName;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ComponentSummary {
        private List<String> skills;
        private List<String> agents;
        private List<String> commands;
        private List<String> hooks;
        /** MCP server 名称列表（解析自 .mcp.json 的 mcpServers keys）。 */
        private List<String> mcpServers;
        private boolean lspServers;
        private boolean hasBin;
        private boolean hasSettings;
    }
}
