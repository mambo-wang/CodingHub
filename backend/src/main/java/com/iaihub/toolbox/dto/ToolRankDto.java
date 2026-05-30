package com.iaihub.toolbox.dto;

public class ToolRankDto {
    private String category;
    private String toolName;
    private Long hotScore;

    public ToolRankDto(String category, String toolName, Long hotScore) {
        this.category = category;
        this.toolName = toolName;
        this.hotScore = hotScore;
    }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getToolName() { return toolName; }
    public void setToolName(String toolName) { this.toolName = toolName; }
    public Long getHotScore() { return hotScore; }
    public void setHotScore(Long hotScore) { this.hotScore = hotScore; }
}