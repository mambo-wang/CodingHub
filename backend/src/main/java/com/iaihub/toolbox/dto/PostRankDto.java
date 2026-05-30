package com.iaihub.toolbox.dto;

public class PostRankDto {
    private String category;
    private String postTitle;
    private Long commentCount;

    public PostRankDto(String category, String postTitle, Long commentCount) {
        this.category = category;
        this.postTitle = postTitle;
        this.commentCount = commentCount;
    }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getPostTitle() { return postTitle; }
    public void setPostTitle(String postTitle) { this.postTitle = postTitle; }
    public Long getCommentCount() { return commentCount; }
    public void setCommentCount(Long commentCount) { this.commentCount = commentCount; }
}