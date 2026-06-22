package com.iaihub.toolbox.dto;

public class StatsDto {
    private Long userCount;
    private Long postCount;
    private Long toolCount;
    private Long videoCount;

    public StatsDto(Long userCount, Long postCount, Long toolCount, Long videoCount) {
        this.userCount = userCount;
        this.postCount = postCount;
        this.toolCount = toolCount;
        this.videoCount = videoCount;
    }

    public Long getUserCount() { return userCount; }
    public Long getPostCount() { return postCount; }
    public Long getToolCount() { return toolCount; }
    public Long getVideoCount() { return videoCount; }
}