package com.iaihub.toolbox.dto.plugin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 插件元数据创建请求（MCP 两段式创建的第一步）。
 *
 * <p>仅保存插件元数据（name/version/description/source），status 置为 DRAFT，
 * 不涉及 zip 文件。随后通过补全接口上传 zip，完成校验后转为 NORMAL 并生成 git 仓库。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PluginCreateDraftRequest {

    @NotBlank(message = "插件名称不能为空")
    @Size(min = 1, max = 100, message = "插件名称长度为1-100字符")
    @Pattern(regexp = "^[a-z0-9]+(-[a-z0-9]+)*$",
             message = "插件名称必须为 kebab-case（小写字母/数字，连字符分隔）")
    private String name;

    @NotBlank(message = "插件版本号不能为空")
    @Pattern(regexp = "^\\d+\\.\\d+\\.\\d+(-[a-zA-Z0-9]+)?$",
             message = "版本号格式不正确，请使用标准格式（如 1.0.0）")
    private String version;

    @Size(max = 5000, message = "插件描述最大5000字符")
    private String description;

    /** 市场引用：GitHub owner/repo 或绝对 URL，选填（未填时使用默认值 internal/local）。 */
    @Size(max = 512, message = "插件 source 最大512字符")
    private String source;
}
