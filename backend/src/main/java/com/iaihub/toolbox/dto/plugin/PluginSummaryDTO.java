package com.iaihub.toolbox.dto.plugin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PluginSummaryDTO {

    private Long id;
    private String name;
    private String description;
    private String version;
    private String logoUrl;
    private String source;
    private Integer likeCount;
    private Integer commentCount;
    private Integer viewCount;
    private BigDecimal score;
    private Long authorId;
    private String authorUsername;
    private String authorNickname;
    private LocalDateTime createdAt;
}
