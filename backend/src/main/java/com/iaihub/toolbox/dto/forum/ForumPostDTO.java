package com.iaihub.toolbox.dto.forum;

import com.iaihub.toolbox.dto.tag.TagDTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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
    LocalDateTime updatedAt,
    BigDecimal score,
    Boolean pinned,
    List<TagDTO> tags
) {}