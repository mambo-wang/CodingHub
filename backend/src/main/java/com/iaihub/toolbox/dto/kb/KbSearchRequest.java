package com.iaihub.toolbox.dto.kb;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KbSearchRequest {

    @NotBlank(message = "搜索查询不能为空")
    private String query;

    @Builder.Default
    private Integer topK = 5;

    private Boolean rerank;

    @Builder.Default
    private Integer expandContext = 0;
}
