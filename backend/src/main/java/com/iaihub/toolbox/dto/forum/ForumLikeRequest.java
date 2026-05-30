package com.iaihub.toolbox.dto.forum;

public record ForumLikeRequest(
    Long postId,
    Long commentId
) {
    public ForumLikeRequest {
        if (postId == null && commentId == null) {
            throw new IllegalArgumentException("postId 和 commentId 至少需要一个");
        }
        if (postId != null && commentId != null) {
            throw new IllegalArgumentException("postId 和 commentId 只能选一个");
        }
    }
}