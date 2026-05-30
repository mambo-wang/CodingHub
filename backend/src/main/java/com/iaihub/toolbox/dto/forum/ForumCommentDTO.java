package com.iaihub.toolbox.dto.forum;

import java.time.LocalDateTime;

public record ForumCommentDTO(
    Long id,
    Long postId,
    Long authorId,
    String authorName,
    Long parentId,
    Long rootId,
    String content,
    Integer likeCount,
    LocalDateTime createdAt
) {}