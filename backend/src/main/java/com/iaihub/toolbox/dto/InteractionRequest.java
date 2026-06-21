package com.iaihub.toolbox.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InteractionRequest {

    @NotBlank(message = "targetType is required")
    private String targetType;

    @NotNull(message = "targetId is required")
    private Long targetId;

    // For comments only
    private String content;
    private Long parentId;
    private String userName; // For anonymous comments
}
