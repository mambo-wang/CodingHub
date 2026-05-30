package com.iaihub.toolbox.dto.forum;

public record ForumCategoryDTO(
    Long id,
    String name,
    String description,
    Integer sortOrder,
    Integer postCount
) {}