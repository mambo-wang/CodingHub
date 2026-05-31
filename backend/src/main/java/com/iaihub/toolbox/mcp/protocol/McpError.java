package com.iaihub.toolbox.mcp.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * MCP 错误模型
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class McpError {

    /**
     * 错误码定义
     */
    public static final int PARSE_ERROR = -32700;
    public static final int INVALID_REQUEST = -32600;
    public static final int METHOD_NOT_FOUND = -32601;
    public static final int INVALID_PARAMS = -32602;
    public static final int INTERNAL_ERROR = -32603;
    public static final int DATABASE_ERROR = -32000;

    @JsonProperty("code")
    private int code;

    @JsonProperty("message")
    private String message;

    @JsonProperty("data")
    private Object data;

    public McpError() {
    }

    public McpError(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public McpError(int code, String message, Object data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static McpError parseError(String message) {
        return new McpError(PARSE_ERROR, message);
    }

    public static McpError invalidRequest(String message) {
        return new McpError(INVALID_REQUEST, message);
    }

    public static McpError methodNotFound(String method) {
        return new McpError(METHOD_NOT_FOUND, "Method not found: " + method);
    }

    public static McpError invalidParams(String message) {
        return new McpError(INVALID_PARAMS, message);
    }

    public static McpError internalError(String message) {
        return new McpError(INTERNAL_ERROR, message);
    }

    public static McpError databaseError(String message) {
        return new McpError(DATABASE_ERROR, message);
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}