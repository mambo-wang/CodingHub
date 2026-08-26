package com.iaihub.toolbox.config;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/tools").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/tools/{id}").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/tools/{id}/like-status").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/tools/hot-top5").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/categories").permitAll()
                // File endpoints (must be before /api/v1/tools/**)
                .requestMatchers(HttpMethod.GET, "/api/v1/tools/{toolId}/files").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/tools/{toolId}/files/{fileId}/download").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/tools/{toolId}/files").permitAll()
                // Avatar static resources & public user profile (no auth needed)
                .requestMatchers(HttpMethod.GET, "/api/v1/static/avatars/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/users/{id}").permitAll()
                // MCP endpoints (streamable-http, 无认证)
                .requestMatchers("/mcp/**").permitAll()
                // Video public endpoints
                .requestMatchers(HttpMethod.GET, "/api/v1/videos").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/videos/{id}").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/videos/{id}/stream").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/videos/{id}/cover-image").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/videos/hot-top5").permitAll()
                // Forum public endpoints
                .requestMatchers(HttpMethod.GET, "/api/forum/posts/hot-top5").permitAll()
                // Tag public endpoints
                .requestMatchers(HttpMethod.GET, "/api/v1/tags").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/tags/hot").permitAll()
                // Plugin marketplace - public read endpoints
                .requestMatchers(HttpMethod.GET, "/api/v1/plugins").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/plugins/{id}").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/plugins/{id}/download").permitAll()
                .requestMatchers("/api/v1/plugin-market/**").permitAll()
                // Built-in git server (Smart HTTP) — 匿名 git clone / fetch
                .requestMatchers("/git/**").permitAll()
                // Unified interactions - likes and comments (public, supports anonymous)
                .requestMatchers(HttpMethod.GET, "/api/v1/interactions/likes/status").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/interactions/likes").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/interactions/comments").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/interactions/comments").permitAll()
                // Unified interactions - comments delete and favorites (require auth)
                .requestMatchers(HttpMethod.DELETE, "/api/v1/interactions/comments/**").authenticated()
                .requestMatchers("/api/v1/interactions/favorites/**").authenticated()
                .requestMatchers("/api/v1/interactions/favorites").authenticated()
                // Admin approval endpoints (SUPER_ADMIN only)
                .requestMatchers("/api/v1/admin/approve/**").hasRole("SUPER_ADMIN")
                .requestMatchers("/api/v1/admin/reject/**").hasRole("SUPER_ADMIN")
                .requestMatchers("/api/v1/admin/pending-users").hasRole("SUPER_ADMIN")
                .requestMatchers("/api/v1/admin/users/*/status").hasRole("SUPER_ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/admin/users/*").hasRole("SUPER_ADMIN")
                // Admin user management (ADMIN and SUPER_ADMIN)
                .requestMatchers("/api/v1/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                // Notification endpoints (require auth)
                .requestMatchers("/api/v1/notifications/**").authenticated()
                // Danmaku - GET is public, POST requires auth
                .requestMatchers(HttpMethod.GET, "/api/v1/videos/{videoId}/danmaku").permitAll()
                // Knowledge base - public read endpoints
                .requestMatchers(HttpMethod.GET, "/api/v1/knowledge").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/knowledge/{id}").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/knowledge/{id}/search").permitAll()
                // Chat - WebSocket and public history
                .requestMatchers("/ws/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/chat/messages").permitAll()
                .requestMatchers(HttpMethod.DELETE, "/api/v1/chat/messages/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                // Feedback - GET and POST are public, admin operations use @PreAuthorize
                .requestMatchers(HttpMethod.GET, "/api/v1/feedback").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/feedback").permitAll()
                // Image upload - GET is public (display in posts), POST requires auth
                .requestMatchers(HttpMethod.GET, "/api/v1/uploads/images/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/uploads/images").authenticated()
                // Protected endpoints
                .requestMatchers("/api/v1/knowledge/**").authenticated()
                .requestMatchers("/api/v1/videos/**").authenticated()
                .requestMatchers("/api/v1/tools/**").authenticated()
                .requestMatchers("/api/v1/plugins/**").authenticated()
                .requestMatchers("/api/v1/users/**").authenticated()
                .anyRequest().permitAll()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setContentType("application/json");
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    Boolean expired = (Boolean) request.getAttribute("jwt.expired");
                    if (Boolean.TRUE.equals(expired)) {
                        response.getWriter().write("{\"error\":\"TOKEN_EXPIRED\",\"message\":\"Token has expired\"}");
                    } else {
                        response.getWriter().write("{\"error\":\"TOKEN_REQUIRED\",\"message\":\"Authentication required\"}");
                    }
                })
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
