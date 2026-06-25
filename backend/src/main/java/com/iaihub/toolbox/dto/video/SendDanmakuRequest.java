package com.iaihub.toolbox.dto.video;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SendDanmakuRequest {

    @NotBlank(message = "弹幕内容不能为空")
    @Size(max = 200, message = "弹幕内容不能超过200个字符")
    private String content;

    private Double timeSeconds;
    private String color;
    private String danmakuType;
}
