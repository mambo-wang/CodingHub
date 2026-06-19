package com.iaihub.toolbox.config;

import com.iaihub.toolbox.model.AccountStatus;
import com.iaihub.toolbox.model.Role;
import com.iaihub.toolbox.model.User;
import com.iaihub.toolbox.repository.UserRepository;
import com.iaihub.toolbox.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UserRepository userRepository;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private Claims claims;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void activeUser_setsAuthenticationWithAuthorities() throws Exception {
        // Arrange
        Long userId = 1L;
        String token = "valid-access-token";
        User user = User.builder()
                .id(userId)
                .username("activeuser")
                .role(Role.USER)
                .status(AccountStatus.ACTIVE)
                .build();

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtUtil.validateToken(token)).thenReturn(true);
        when(jwtUtil.parseToken(token)).thenReturn(claims);
        when(claims.get("type")).thenReturn("access");
        when(claims.getSubject()).thenReturn(userId.toString());
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals(user, auth.getPrincipal());
        assertEquals(1, auth.getAuthorities().size());
        assertTrue(auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void pendingUser_doesNotSetAuthentication() throws Exception {
        // Arrange
        Long userId = 2L;
        String token = "pending-user-token";
        User user = User.builder()
                .id(userId)
                .username("pendinguser")
                .role(Role.USER)
                .status(AccountStatus.PENDING)
                .build();

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtUtil.validateToken(token)).thenReturn(true);
        when(jwtUtil.parseToken(token)).thenReturn(claims);
        when(claims.get("type")).thenReturn("access");
        when(claims.getSubject()).thenReturn(userId.toString());
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void disabledUser_doesNotSetAuthentication() throws Exception {
        // Arrange
        Long userId = 3L;
        String token = "disabled-user-token";
        User user = User.builder()
                .id(userId)
                .username("disableduser")
                .role(Role.USER)
                .status(AccountStatus.DISABLED)
                .build();

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtUtil.validateToken(token)).thenReturn(true);
        when(jwtUtil.parseToken(token)).thenReturn(claims);
        when(claims.get("type")).thenReturn("access");
        when(claims.getSubject()).thenReturn(userId.toString());
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void noToken_continuesFilterChain() throws Exception {
        // Arrange — no Authorization header
        when(request.getHeader("Authorization")).thenReturn(null);

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(jwtUtil, never()).validateToken(anyString());
        verify(jwtUtil, never()).parseToken(anyString());
        verify(userRepository, never()).findById(anyLong());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void invalidToken_continuesFilterChain() throws Exception {
        // Arrange
        String badToken = "invalid-token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + badToken);
        when(jwtUtil.validateToken(badToken)).thenReturn(false);

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(jwtUtil).validateToken(badToken);
        verify(jwtUtil, never()).parseToken(anyString());
        verify(userRepository, never()).findById(anyLong());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void refreshToken_doesNotSetAuthentication() throws Exception {
        // Arrange
        Long userId = 4L;
        String token = "refresh-type-token";

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtUtil.validateToken(token)).thenReturn(true);
        when(jwtUtil.parseToken(token)).thenReturn(claims);
        when(claims.get("type")).thenReturn("refresh");

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(userRepository, never()).findById(anyLong());
        verify(filterChain).doFilter(request, response);
    }
}
