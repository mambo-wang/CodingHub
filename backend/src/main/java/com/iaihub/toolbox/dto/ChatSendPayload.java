package com.iaihub.toolbox.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatSendPayload {
    private String roomId;
    private String content;
    private String displayName;
    private Long replyTo;

    public ChatSendPayload(String roomId, String content, String displayName) {
        this.roomId = roomId;
        this.content = content;
        this.displayName = displayName;
        this.replyTo = null;
    }
}
