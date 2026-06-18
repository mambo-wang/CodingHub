package com.iaihub.toolbox.dto.video;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoCommentRequest {

    @NotBlank(message = "评论内容不能为空")
    private String content;
}
