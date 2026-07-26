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
}
