package com.iaihub.toolbox.dto.video;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoCommentResponse {

    private Long id;
    private String content;
    private Long userId;
    private String userNickname;
    private String userAvatarUrl;
    private LocalDateTime createdAt;
}
