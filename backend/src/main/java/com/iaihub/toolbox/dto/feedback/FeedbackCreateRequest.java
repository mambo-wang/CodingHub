package com.iaihub.toolbox.dto.feedback;

import jakarta.validation.constraints.NotBlank;

public record FeedbackCreateRequest(
    @NotBlank(message = "留言内容不能为空")
    String content,
    String nickname,
    String contact,
    String category
) {}
