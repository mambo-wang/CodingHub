package com.iaihub.toolbox.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PendingUserDTO {
    private Long id;
    private String username;
    private String nickname;
    private String role;
    private LocalDateTime createdAt;
}
