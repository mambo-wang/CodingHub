package com.iaihub.toolbox.dto;

import java.math.BigDecimal;

public class PostRankDto {
    private Long id;
    private String category;
    private String postTitle;
    private BigDecimal score;

    public PostRankDto(Long id, String category, String postTitle, BigDecimal score) {
        this.id = id;
        this.category = category;
        this.postTitle = postTitle;
        this.score = score;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getPostTitle() { return postTitle; }
    public void setPostTitle(String postTitle) { this.postTitle = postTitle; }
    public BigDecimal getScore() { return score; }
    public void setScore(BigDecimal score) { this.score = score; }
}