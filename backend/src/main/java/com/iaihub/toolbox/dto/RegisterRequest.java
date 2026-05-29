package com.iaihub.toolbox.dto;

import jakarta.validation.constraints.Email;
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

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @NotBlank(message = "密码不能为空")
    @Size(min = 8, message = "密码至少8个字符")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
             message = "密码必须包含大小写字母和数字")
    private String password;

    @NotBlank(message = "用户名不能为空")
    @Size(min = 1, max = 100, message = "用户名长度为1-100字符")
    @Pattern(regexp = "^[a-zA-Z0-9\\u4e00-\\u9fa5_-]+$",
             message = "用户名只能包含字母、数字、中文、下划线和连字符")
    private String username;
}
