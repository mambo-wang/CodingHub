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
public class ToolDetailDTO {

    private Long id;
    private String name;
    private String version;
    private String description;
    private String categoryName;
    private String categoryIcon;
    private String logoUrl;
    private String content;
    private Long uploaderId;
    private String uploaderUsername;
    private String uploaderNickname;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer viewCount;
    private Integer likeCount;
    private Integer commentCount;
    private Integer favoriteCount;
    private Integer downloadCount;
    private BigDecimal score;
    private List<com.iaihub.toolbox.dto.tag.TagDTO> tags;
}