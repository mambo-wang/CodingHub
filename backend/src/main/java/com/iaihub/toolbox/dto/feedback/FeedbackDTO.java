package com.iaihub.toolbox.dto.feedback;

import java.time.LocalDateTime;

public record FeedbackDTO(
    Long id,
    String content,
    String nickname,
    String contact,
    String category,
    LocalDateTime createdAt,
    String adminReply,
    LocalDateTime repliedAt
) {}
