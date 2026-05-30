package com.iaihub.toolbox.dto.forum;

public record ForumTagDTO(
    Long id,
    String name,
    Integer postCount,
    Boolean isSystem
) {}