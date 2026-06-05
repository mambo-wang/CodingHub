package com.iaihub.toolbox.dto.forum;

import java.time.LocalDateTime;

public record ForumCommentDTO(
    Long id,
    Long postId,
    Long authorId,
    String authorName,
    String authorNickname,
    Long parentId,
    Long rootId,
    String content,
    Integer likeCount,
    LocalDateTime createdAt
) {}