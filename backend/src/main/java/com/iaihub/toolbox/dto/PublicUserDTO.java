package com.iaihub.toolbox.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 公开用户信息（不含密码、email、lastLoginAt 等敏感字段）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublicUserDTO {
    private Long id;
    private String username;
    private String nickname;
    private String avatarUrl;
    private LocalDateTime createdAt;
}
