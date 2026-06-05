package com.iaihub.toolbox.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 工具搜索结果 DTO
 */
public class ToolSearchResult {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("category")
    private String category;

    @JsonProperty("version")
    private String version;

    @JsonProperty("createdAt")
    private String createdAt;

    public ToolSearchResult() {
    }

    public ToolSearchResult(Long id, String name, String description, String category, String version, String createdAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.category = category;
        this.version = version;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}