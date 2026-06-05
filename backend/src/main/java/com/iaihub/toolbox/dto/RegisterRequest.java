package com.iaihub.toolbox.dto;

import jakarta.validation.constraints.NotBlank;
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
public class RegisterRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 4, max = 20, message = "用户名长度为4-20字符")
    @Pattern(regexp = "^\\w+$", message = "用户名只能包含字母、数字、下划线")
    private String username;

    @NotBlank(message = "昵称不能为空")
    @Size(min = 2, max = 10, message = "昵称长度为2-10字符")
    @Pattern(regexp = "^[\\u4e00-\\u9fa5a-zA-Z0-9]+$", message = "昵称只能包含中文、字母、数字")
    private String nickname;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, message = "密码长度至少6位")
    private String password;
}