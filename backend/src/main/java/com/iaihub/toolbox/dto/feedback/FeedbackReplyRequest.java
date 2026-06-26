package com.iaihub.toolbox.dto.feedback;

import jakarta.validation.constraints.NotBlank;

public record FeedbackReplyRequest(
    @NotBlank(message = "回复内容不能为空")
    String adminReply
) {}
