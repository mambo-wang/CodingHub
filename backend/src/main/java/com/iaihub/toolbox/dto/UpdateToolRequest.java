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

    @NotBlank(message = "工具名称不能为空")
    @Size(min = 1, max = 100, message = "工具名称长度为1-100字符")
    @Pattern(regexp = "^[a-zA-Z0-9\\u4e00-\\u9fa5_-]+$",
             message = "工具名称只能包含字母、数字、中文、下划线和连字符")
    private String name;

    @NotNull(message = "分类ID不能为空")
    private Long categoryId;

    @NotBlank(message = "介绍内容不能为空")
    @Size(max = 5000, message = "介绍内容最大5000字符")
    private String content;
}
