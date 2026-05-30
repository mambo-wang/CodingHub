package com.iaihub.toolbox.dto.forum;

import jakarta.validation.constraints.NotBlank;

public record ForumCommentCreateRequest(
    @NotBlank String content,
    Long parentId,
    String authorName
) {}