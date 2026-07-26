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

    private Long replyTo;
    private String replyToDisplayName;
    private String replyToContentPreview;
    private boolean edited;
    private String deletedType; // ADMIN / SELF
    private java.util.Map<String, Integer> reactions;
    private java.util.List<String> myReactions;
}
