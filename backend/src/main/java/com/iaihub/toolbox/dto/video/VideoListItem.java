package com.iaihub.toolbox.dto.video;

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
public class VideoListItem {

    private Long id;
    private String title;
    private String coverUrl;
    private Integer duration;
    private Integer viewCount;
    private Integer likeCount;
    private Integer commentCount;
    private Long uploaderId;
    private String uploaderName;
    private String uploaderNickname;
    private String uploaderAvatarUrl;
    private LocalDateTime createdAt;
    private BigDecimal score;
    private Boolean pinned;
    private List<com.iaihub.toolbox.dto.tag.TagDTO> tags;
}
