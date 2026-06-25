package com.iaihub.toolbox.dto.video;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoResponse {

    private Long id;
    private String title;
    private String description;
    private String coverUrl;
    private Integer duration;
    private Long fileSize;
    private Integer viewCount;
    private Integer likeCount;
    private Integer commentCount;
    private Long uploaderId;
    private String uploaderName;
    private String uploaderNickname;
    private String uploaderAvatarUrl;
    private Boolean userLiked;
    private Boolean userFavorited;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<com.iaihub.toolbox.dto.tag.TagDTO> tags;
}
