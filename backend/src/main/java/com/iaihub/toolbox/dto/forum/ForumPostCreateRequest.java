package com.iaihub.toolbox.dto.forum;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record ForumPostCreateRequest(
    @NotBlank String title,
    @NotBlank String content,
    @NotNull Long categoryId,
    List<Long> tagIds
) {}