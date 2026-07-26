package com.iaihub.toolbox.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.security.Principal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatPrincipal implements Principal {

    private Long userId;
    private String displayName;
    private String avatarUrl;
    private String ipHash;
    private boolean admin;
    private String sessionId;

    @Override
    public String getName() {
        return userId != null ? userId.toString() : "guest:" + sessionId;
    }
}
