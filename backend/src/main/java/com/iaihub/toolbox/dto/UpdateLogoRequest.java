package com.iaihub.toolbox.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateLogoRequest {

    @Size(max = 512, message = "Logo地址最大512字符")
    private String logoUrl;
}
