package com.iaihub.toolbox.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageDTO {
    private Long id;
    private String roomId;
    private Long userId;
    private String displayName;
    private String avatarUrl;
    private String content;
    private String status;
    private LocalDateTime createdAt;
    private boolean guest;
}
