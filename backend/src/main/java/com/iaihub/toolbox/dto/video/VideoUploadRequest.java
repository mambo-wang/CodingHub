package com.iaihub.toolbox.dto.video;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoUploadRequest {

    @NotBlank(message = "视频标题不能为空")
    @Size(max = 200, message = "视频标题不能超过200字符")
    private String title;

    private String description;
}
