package com.iaihub.toolbox.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReactionActionPayload {
    private Long messageId;
    private String emoji;
}
