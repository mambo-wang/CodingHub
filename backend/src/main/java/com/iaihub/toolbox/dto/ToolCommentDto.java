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
public class ToolCommentDto {
    private Long id;
    private String content;
    private String username;
    private LocalDateTime createdAt;
}