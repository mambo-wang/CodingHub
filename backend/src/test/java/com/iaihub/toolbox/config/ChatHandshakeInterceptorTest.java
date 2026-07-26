package com.iaihub.toolbox.config;

import com.iaihub.toolbox.model.AccountStatus;
import com.iaihub.toolbox.model.Role;
import com.iaihub.toolbox.model.User;
import com.iaihub.toolbox.repository.UserRepository;
import com.iaihub.toolbox.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatHandshakeInterceptorTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ChatHandshakeInterceptor interceptor;

    @Test
    void validToken_createsLoggedInPrincipal() throws Exception {
        String token = "valid-jwt";
        User user = User.builder()
                .id(1L).username("testuser").nickname("TestNick")
                .role(Role.USER).status(AccountStatus.ACTIVE).build();

        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.setParameter("token", token);
        servletRequest.setRemoteAddr("127.0.0.1");
        ServletServerHttpRequest request = new ServletServerHttpRequest(servletRequest);

        when(jwtUtil.validateToken(token)).thenReturn(true);
        when(jwtUtil.getUserIdFromToken(token)).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        Map<String, Object> attributes = new HashMap<>();
        boolean result = interceptor.beforeHandshake(request, null, null, attributes);

        assertTrue(result);
        ChatPrincipal principal = (ChatPrincipal) attributes.get("principal");
        assertNotNull(principal);
        assertEquals(1L, principal.getUserId());
        assertEquals("TestNick", principal.getDisplayName());
        assertFalse(principal.isAdmin());
    }

    @Test
    void invalidToken_createsGuestPrincipal() throws Exception {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.setParameter("token", "bad-token");
        servletRequest.setRemoteAddr("127.0.0.1");
        ServletServerHttpRequest request = new ServletServerHttpRequest(servletRequest);

        when(jwtUtil.validateToken("bad-token")).thenReturn(false);

        Map<String, Object> attributes = new HashMap<>();
        boolean result = interceptor.beforeHandshake(request, null, null, attributes);

        assertTrue(result);
        ChatPrincipal principal = (ChatPrincipal) attributes.get("principal");
        assertNotNull(principal);
        assertNull(principal.getUserId());
        assertFalse(principal.isAdmin());
    }

    @Test
    void noToken_createsGuestPrincipal() throws Exception {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.setRemoteAddr("192.168.1.1");
        ServletServerHttpRequest request = new ServletServerHttpRequest(servletRequest);

        Map<String, Object> attributes = new HashMap<>();
        boolean result = interceptor.beforeHandshake(request, null, null, attributes);

        assertTrue(result);
        ChatPrincipal principal = (ChatPrincipal) attributes.get("principal");
        assertNotNull(principal);
        assertNull(principal.getUserId());
        assertNotNull(principal.getIpHash());
    }

    @Test
    void adminUser_principalHasAdminTrue() throws Exception {
        String token = "admin-jwt";
        User admin = User.builder()
                .id(2L).username("admin").nickname("Admin")
                .role(Role.ADMIN).status(AccountStatus.ACTIVE).build();

        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.setParameter("token", token);
        servletRequest.setRemoteAddr("127.0.0.1");
        ServletServerHttpRequest request = new ServletServerHttpRequest(servletRequest);

        when(jwtUtil.validateToken(token)).thenReturn(true);
        when(jwtUtil.getUserIdFromToken(token)).thenReturn(2L);
        when(userRepository.findById(2L)).thenReturn(Optional.of(admin));

        Map<String, Object> attributes = new HashMap<>();
        interceptor.beforeHandshake(request, null, null, attributes);

        ChatPrincipal principal = (ChatPrincipal) attributes.get("principal");
        assertTrue(principal.isAdmin());
    }
}
