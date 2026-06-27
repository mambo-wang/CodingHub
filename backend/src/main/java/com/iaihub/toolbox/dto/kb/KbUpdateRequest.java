package com.iaihub.toolbox.dto.kb;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KbUpdateRequest {

    @Size(max = 100, message = "知识库名称最长100个字符")
    private String name;

    @Size(max = 500, message = "描述最长500个字符")
    private String description;
}
