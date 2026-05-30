package com.iaihub.toolbox.dto;

public class StatsDto {
    private Long userCount;
    private Long postCount;
    private Long toolCount;

    public StatsDto(Long userCount, Long postCount, Long toolCount) {
        this.userCount = userCount;
        this.postCount = postCount;
        this.toolCount = toolCount;
    }

    public Long getUserCount() { return userCount; }
    public Long getPostCount() { return postCount; }
    public Long getToolCount() { return toolCount; }
}