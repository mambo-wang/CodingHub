package com.iaihub.toolbox.dto.video;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DanmakuDTO {
    private Long id;
    private Long userId;
    private String username;
    private String nickname;
    private String content;
    private Double timeSeconds;
    private String color;
    private String danmakuType;
}
