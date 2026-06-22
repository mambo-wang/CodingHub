package com.iaihub.toolbox.dto;

public class VideoRankDto {
    private Long id;
    private String videoTitle;
    private Integer viewCount;
    private Integer likeCount;

    public VideoRankDto(Long id, String videoTitle, Integer viewCount, Integer likeCount) {
        this.id = id;
        this.videoTitle = videoTitle;
        this.viewCount = viewCount;
        this.likeCount = likeCount;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getVideoTitle() { return videoTitle; }
    public void setVideoTitle(String videoTitle) { this.videoTitle = videoTitle; }
    public Integer getViewCount() { return viewCount; }
    public void setViewCount(Integer viewCount) { this.viewCount = viewCount; }
    public Integer getLikeCount() { return likeCount; }
    public void setLikeCount(Integer likeCount) { this.likeCount = likeCount; }
}
