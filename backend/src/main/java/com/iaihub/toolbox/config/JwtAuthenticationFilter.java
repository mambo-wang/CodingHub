package com.iaihub.toolbox.config;

import com.iaihub.toolbox.model.AccountStatus;
import com.iaihub.toolbox.model.User;
import com.iaihub.toolbox.repository.UserRepository;
import com.iaihub.toolbox.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String jwt = extractJwtFromRequest(request);

            if (StringUtils.hasText(jwt)) {
                // Distinguish expired token from invalid token
                if (!jwtUtil.validateToken(jwt)) {
                    if (jwtUtil.isTokenExpired(jwt)) {
                        // Mark as 401 so authenticationEntryPoint returns correct status
                        log.warn("Expired JWT token for request: {} {}", request.getMethod(), request.getRequestURI());
                        request.setAttribute("jwt.expired", true);
                    }
                    // Expired or invalid — continue without authentication
                } else {
                    Claims claims = jwtUtil.parseToken(jwt);
                    String tokenType = (String) claims.get("type");

                    if (!"access".equals(tokenType)) {
                        log.warn("Invalid token type: {}", tokenType);
                    } else {
                        Long userId = Long.parseLong(claims.getSubject());
                        Optional<User> userOpt = userRepository.findById(userId);

                        if (userOpt.isPresent()) {
                            User user = userOpt.get();

                            // Only set authentication for ACTIVE users
                            if (user.getStatus() == AccountStatus.ACTIVE) {
                                List<SimpleGrantedAuthority> authorities = List.of(
                                        new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
                                );
                                UsernamePasswordAuthenticationToken authentication =
                                        new UsernamePasswordAuthenticationToken(user, null, authorities);
                                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                                SecurityContextHolder.getContext().setAuthentication(authentication);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Cannot set user authentication: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
