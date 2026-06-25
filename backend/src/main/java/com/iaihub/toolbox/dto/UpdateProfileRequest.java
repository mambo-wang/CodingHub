package com.iaihub.toolbox.dto;

import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateProfileRequest {

    @Size(max = 50, message = "昵称不能超过50个字符")
    private String nickname;

    @Size(max = 500, message = "简介不能超过500个字符")
    private String bio;
}
