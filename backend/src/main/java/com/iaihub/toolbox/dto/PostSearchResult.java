package com.iaihub.toolbox.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 帖子搜索结果 DTO
 */
public class PostSearchResult {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("title")
    private String title;

    @JsonProperty("summary")
    private String summary;

    @JsonProperty("authorName")
    private String authorName;

    @JsonProperty("createdAt")
    private String createdAt;

    public PostSearchResult() {
    }

    public PostSearchResult(Long id, String title, String summary, String authorName, String createdAt) {
        this.id = id;
        this.title = title;
        this.summary = summary;
        this.authorName = authorName;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}