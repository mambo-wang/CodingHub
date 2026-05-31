package com.iaihub.toolbox.dto;

import java.math.BigDecimal;

public class ToolRankDto {
    private Long id;
    private String category;
    private String toolName;
    private BigDecimal score;

    public ToolRankDto(Long id, String category, String toolName, BigDecimal score) {
        this.id = id;
        this.category = category;
        this.toolName = toolName;
        this.score = score;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getToolName() { return toolName; }
    public void setToolName(String toolName) { this.toolName = toolName; }
    public BigDecimal getScore() { return score; }
    public void setScore(BigDecimal score) { this.score = score; }
}