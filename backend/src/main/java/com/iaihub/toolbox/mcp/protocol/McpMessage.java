package com.iaihub.toolbox.mcp.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * MCP 消息模型（JSON-RPC 2.0）
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class McpMessage {

    @JsonProperty("jsonrpc")
    private String jsonrpc = "2.0";

    @JsonProperty("id")
    private Integer id;

    @JsonProperty("method")
    private String method;

    @JsonProperty("params")
    private Map<String, Object> params;

    public McpMessage() {
    }

    public McpMessage(String method, Map<String, Object> params, Integer id) {
        this.jsonrpc = "2.0";
        this.method = method;
        this.params = params;
        this.id = id;
    }

    public String getJsonrpc() {
        return jsonrpc;
    }

    public void setJsonrpc(String jsonrpc) {
        this.jsonrpc = jsonrpc;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params;
    }

    /**
     * 解析方法名中的工具名称
     * 如 "tools/call" -> "tools/call", "tools/list" -> "tools/list"
     */
    public String getToolName() {
        if (method != null && method.contains("/")) {
            return method;
        }
        return method;
    }

    /**
     * 检查是否为工具调用请求
     */
    public boolean isToolsCall() {
        return "tools/call".equals(method);
    }

    /**
     * 检查是否为工具列表请求
     */
    public boolean isToolsList() {
        return "tools/list".equals(method);
    }
}