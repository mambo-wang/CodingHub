package com.iaihub.toolbox.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileListResponse {

    private Long toolId;
    private String folderPath;
    private List<ToolFileDTO> files;
    private boolean readmeExists;
}
