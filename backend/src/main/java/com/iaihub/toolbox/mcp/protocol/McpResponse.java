package com.iaihub.toolbox.mcp.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * MCP 响应模型（JSON-RPC 2.0）
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class McpResponse {

    @JsonProperty("jsonrpc")
    private String jsonrpc = "2.0";

    @JsonProperty("id")
    private Integer id;

    @JsonProperty("result")
    private Object result;

    @JsonProperty("error")
    private McpError error;

    public McpResponse() {
    }

    public static McpResponse success(Integer id, Object result) {
        McpResponse response = new McpResponse();
        response.setId(id);
        response.setResult(result);
        return response;
    }

    public static McpResponse error(Integer id, McpError error) {
        McpResponse response = new McpResponse();
        response.setId(id);
        response.setError(error);
        return response;
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

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public McpError getError() {
        return error;
    }

    public void setError(McpError error) {
        this.error = error;
    }
}