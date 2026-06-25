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
public class UserDTO {

    private Long id;
    private String username;
    private String nickname;
    private String avatarUrl;
    private String bio;
    private String role;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
}
