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
public class ToolFileDTO {

    private Long id;
    private Long toolId;
    private String originalName;
    private String storedPath;
    private Long fileSize;
    private String contentType;
    private LocalDateTime createdAt;
}
