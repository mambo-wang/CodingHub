package com.iaihub.toolbox.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * MCP 搜索请求 DTO
 */
public class McpSearchRequest {

    @Size(max = 200, message = "Query must be at most 200 characters")
    private String query;

    private String category;

    @Min(value = 1, message = "Limit must be at least 1")
    @Max(value = 100, message = "Limit must be at most 100")
    private Integer limit = 20;

    public McpSearchRequest() {
    }

    public McpSearchRequest(String query, String category, Integer limit) {
        this.query = query;
        this.category = category;
        this.limit = limit != null ? limit : 20;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit != null ? limit : 20;
    }
}