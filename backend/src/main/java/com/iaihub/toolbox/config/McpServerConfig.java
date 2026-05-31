package com.iaihub.toolbox.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * MCP Server 自动配置类
 * 在独立端口（默认 8081）启动 MCP Server
 */
@Configuration
@ConfigurationProperties(prefix = "mcp.server")
public class McpServerConfig {

    private int port = 8081;
    private String host = "0.0.0.0";
    private boolean enabled = true;
    private int maxConnections = 10;
    private int connectionTimeoutMs = 30000;

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getMaxConnections() {
        return maxConnections;
    }

    public void setMaxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
    }

    public int getConnectionTimeoutMs() {
        return connectionTimeoutMs;
    }

    public void setConnectionTimeoutMs(int connectionTimeoutMs) {
        this.connectionTimeoutMs = connectionTimeoutMs;
    }
}