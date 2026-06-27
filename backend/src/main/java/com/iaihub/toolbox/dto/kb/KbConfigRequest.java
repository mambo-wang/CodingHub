package com.iaihub.toolbox.dto.kb;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KbConfigRequest {

    private String chunkMode;
    private Integer chunkSize;
    private Integer chunkOverlap;
    private Boolean rerank;
    private String description;
}
