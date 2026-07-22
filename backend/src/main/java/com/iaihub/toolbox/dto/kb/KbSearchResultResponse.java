package com.iaihub.toolbox.dto.kb;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KbSearchResultResponse {

    private String text;
    private String source;
    private double score;
    private Integer chunkIndex;
    private String contextHeader;
}
