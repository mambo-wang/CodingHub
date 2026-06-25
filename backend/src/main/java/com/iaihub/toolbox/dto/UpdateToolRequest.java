package com.iaihub.toolbox.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateToolRequest {

    @Size(min = 1, max = 100, message = "工具名称长度为1-100字符")
    @Pattern(regexp = "^[a-zA-Z0-9\\u4e00-\\u9fa5_-]+$",
             message = "工具名称只能包含字母、数字、中文、下划线和连字符")
    private String name;

    private Long categoryId;

    @Size(max = 5000, message = "介绍内容最大5000字符")
    private String content;

    @Size(max = 200, message = "简短描述最大200字符")
    private String description;

    @Pattern(regexp = "^\\d+\\.\\d+\\.\\d+(-[a-zA-Z0-9]+)?$",
             message = "版本号格式不正确，请使用标准格式（如 1.0.0）")
    private String version;

    private java.util.List<Long> tagIds;
}
