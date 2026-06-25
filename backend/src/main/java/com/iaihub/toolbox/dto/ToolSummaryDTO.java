package com.iaihub.toolbox.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ToolSummaryDTO {

    private Long id;
    private String name;
    private String version;
    private String description;
    private String categoryName;
    private String categoryIcon;
    private Long uploaderId;
    private String uploaderUsername;
    private String uploaderNickname;
    private LocalDateTime createdAt;
    private BigDecimal score;
    private Boolean pinned;
    private Integer viewCount;
    private Integer likeCount;
    private Integer commentCount;
    private List<com.iaihub.toolbox.dto.tag.TagDTO> tags;
}
