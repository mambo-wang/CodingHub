package com.iaihub.toolbox.dto.forum;

import java.time.LocalDateTime;

public record ForumPostDTO(
    Long id,
    String title,
    String content,
    Long authorId,
    String authorName,
    String authorNickname,
    Long categoryId,
    String categoryName,
    Integer viewCount,
    Integer likeCount,
    Integer commentCount,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}