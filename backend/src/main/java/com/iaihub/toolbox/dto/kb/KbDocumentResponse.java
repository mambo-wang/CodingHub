package com.iaihub.toolbox.dto.kb;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KbDocumentResponse {

    private Long id;
    private Long kbId;
    private String originalName;
    private Long fileSize;
    private Integer chunkCount;
    private String chunkMode;
    private String uploaderNickname;
    private LocalDateTime createdAt;
}
