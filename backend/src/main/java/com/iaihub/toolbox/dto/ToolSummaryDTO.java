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
public class ToolSummaryDTO {

    private Long id;
    private String name;
    private String version;
    private String categoryName;
    private String categoryIcon;
    private String uploaderUsername;
    private String uploaderNickname;
    private LocalDateTime createdAt;
}
