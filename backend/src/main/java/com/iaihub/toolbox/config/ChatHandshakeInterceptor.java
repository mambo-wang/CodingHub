package com.iaihub.toolbox.config;

import com.iaihub.toolbox.model.AccountStatus;
import com.iaihub.toolbox.model.User;
import com.iaihub.toolbox.repository.UserRepository;
import com.iaihub.toolbox.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String token = null;
        String clientIp = "unknown";

        if (request instanceof ServletServerHttpRequest servletRequest) {
            token = servletRequest.getServletRequest().getParameter("token");
            clientIp = servletRequest.getServletRequest().getRemoteAddr();
            String forwarded = servletRequest.getServletRequest().getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isEmpty()) {
                clientIp = forwarded.split(",")[0].trim();
            }
        }

        String ipHash = computeIpHash(clientIp);
        String sessionId = request.getHeaders().getFirst("Sec-WebSocket-Key");
        if (sessionId == null) {
            sessionId = String.valueOf(System.nanoTime());
        }

        ChatPrincipal principal;

        if (token != null && !token.isBlank() && jwtUtil.validateToken(token)) {
            try {
                Long userId = jwtUtil.getUserIdFromToken(token);
                Optional<User> userOpt = userRepository.findById(userId);
                if (userOpt.isPresent() && userOpt.get().getStatus() == AccountStatus.ACTIVE) {
                    User user = userOpt.get();
                    principal = ChatPrincipal.builder()
                            .userId(userId)
                            .displayName(user.getNickname() != null ? user.getNickname() : user.getUsername())
                            .avatarUrl(user.getAvatarUrl())
                            .ipHash(ipHash)
                            .admin(user.getRole() == com.iaihub.toolbox.model.Role.ADMIN
                                    || user.getRole() == com.iaihub.toolbox.model.Role.SUPER_ADMIN)
                            .sessionId(sessionId)
                            .build();
                } else {
                    principal = buildGuestPrincipal(ipHash, sessionId);
                }
            } catch (Exception e) {
                log.warn("Failed to parse JWT for WebSocket handshake: {}", e.getMessage());
                principal = buildGuestPrincipal(ipHash, sessionId);
            }
        } else {
            principal = buildGuestPrincipal(ipHash, sessionId);
        }

        attributes.put("principal", principal);
        attributes.put("ipHash", ipHash);
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // No-op
    }

    private ChatPrincipal buildGuestPrincipal(String ipHash, String sessionId) {
        return ChatPrincipal.builder()
                .userId(null)
                .displayName(null)
                .avatarUrl(null)
                .ipHash(ipHash)
                .admin(false)
                .sessionId(sessionId)
                .build();
    }

    private String computeIpHash(String ip) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(ip.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            log.error("Failed to compute IP hash", e);
            return "unknown";
        }
    }
}
