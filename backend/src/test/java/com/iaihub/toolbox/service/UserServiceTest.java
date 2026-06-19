package com.iaihub.toolbox.service;

import com.iaihub.toolbox.config.UploadConfig;
import com.iaihub.toolbox.dto.ApprovalResponse;
import com.iaihub.toolbox.dto.LoginRequest;
import com.iaihub.toolbox.dto.LoginResponse;
import com.iaihub.toolbox.dto.RegisterRequest;
import com.iaihub.toolbox.exception.DuplicateResourceException;
import com.iaihub.toolbox.exception.ForbiddenException;
import com.iaihub.toolbox.exception.UnauthorizedException;
import com.iaihub.toolbox.model.AccountStatus;
import com.iaihub.toolbox.model.Role;
import com.iaihub.toolbox.model.User;
import com.iaihub.toolbox.repository.UserRepository;
import com.iaihub.toolbox.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UploadConfig uploadConfig;

    private UserService userService;

    private User activeUser;
    private User pendingUser;
    private User rejectedUser;
    private User disabledUser;
    private User superAdminUser;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, passwordEncoder, jwtUtil, uploadConfig);

        activeUser = User.builder()
                .id(1L)
                .username("testuser")
                .nickname("Test")
                .password("encodedPassword")
                .role(Role.USER)
                .status(AccountStatus.ACTIVE)
                .build();

        pendingUser = User.builder()
                .id(2L)
                .username("pendinguser")
                .nickname("Pending")
                .password("encodedPassword")
                .role(Role.ADMIN)
                .status(AccountStatus.PENDING)
                .build();

        rejectedUser = User.builder()
                .id(3L)
                .username("rejecteduser")
                .nickname("Rejected")
                .password("encodedPassword")
                .role(Role.ADMIN)
                .status(AccountStatus.REJECTED)
                .build();

        disabledUser = User.builder()
                .id(4L)
                .username("disableduser")
                .nickname("Disabled")
                .password("encodedPassword")
                .role(Role.USER)
                .status(AccountStatus.DISABLED)
                .build();

        superAdminUser = User.builder()
                .id(5L)
                .username("superadmin")
                .nickname("SuperAdmin")
                .password("encodedPassword")
                .role(Role.SUPER_ADMIN)
                .status(AccountStatus.ACTIVE)
                .build();
    }

    // ===== Register Tests =====

    @Test
    void register_userRole_returnsActiveWithToken() {
        RegisterRequest request = RegisterRequest.builder()
                .username("newuser")
                .nickname("NewUser")
                .password("password123")
                .role("USER")
                .build();

        lenient().when(userRepository.existsByUsername("newuser")).thenReturn(false);
        lenient().when(userRepository.existsByNickname("NewUser")).thenReturn(false);
        lenient().when(passwordEncoder.encode("password123")).thenReturn("encodedPw");
        lenient().when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(10L);
            return u;
        });
        lenient().when(jwtUtil.generateAccessToken(10L, "newuser")).thenReturn("access-token");
        lenient().when(jwtUtil.generateRefreshToken(10L, "newuser")).thenReturn("refresh-token");

        LoginResponse response = userService.register(request);

        assertEquals("ACTIVE", response.getUser().getStatus());
        assertEquals("USER", response.getUser().getRole());
        assertEquals("access-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User savedUser = captor.getValue();
        assertEquals(AccountStatus.ACTIVE, savedUser.getStatus());
        assertEquals(Role.USER, savedUser.getRole());
    }

    @Test
    void register_adminRole_returnsPendingWithoutToken() {
        RegisterRequest request = RegisterRequest.builder()
                .username("newadmin")
                .nickname("NewAdmin")
                .password("password123")
                .role("ADMIN")
                .build();

        lenient().when(userRepository.existsByUsername("newadmin")).thenReturn(false);
        lenient().when(userRepository.existsByNickname("NewAdmin")).thenReturn(false);
        lenient().when(passwordEncoder.encode("password123")).thenReturn("encodedPw");
        lenient().when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(20L);
            return u;
        });

        LoginResponse response = userService.register(request);

        assertEquals("PENDING", response.getUser().getStatus());
        assertEquals("ADMIN", response.getUser().getRole());
        assertNull(response.getAccessToken());
        assertNull(response.getRefreshToken());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User savedUser = captor.getValue();
        assertEquals(AccountStatus.PENDING, savedUser.getStatus());
        assertEquals(Role.ADMIN, savedUser.getRole());
    }

    @Test
    void register_superAdminRole_throwsException() {
        RegisterRequest request = RegisterRequest.builder()
                .username("superadmin")
                .nickname("SuperAdmin")
                .password("password123")
                .role("SUPER_ADMIN")
                .build();

        lenient().when(userRepository.existsByUsername("superadmin")).thenReturn(false);
        lenient().when(userRepository.existsByNickname("SuperAdmin")).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userService.register(request));
        assertEquals("不允许注册超级管理员", ex.getMessage());
    }

    @Test
    void register_duplicateUsername_throwsException() {
        RegisterRequest request = RegisterRequest.builder()
                .username("testuser")
                .nickname("Unique")
                .password("password123")
                .build();

        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        DuplicateResourceException ex = assertThrows(DuplicateResourceException.class,
                () -> userService.register(request));
        assertEquals("该用户名已被注册", ex.getMessage());
    }

    @Test
    void register_duplicateNickname_throwsException() {
        RegisterRequest request = RegisterRequest.builder()
                .username("uniqueuser")
                .nickname("Test")
                .password("password123")
                .build();

        lenient().when(userRepository.existsByUsername("uniqueuser")).thenReturn(false);
        when(userRepository.existsByNickname("Test")).thenReturn(true);

        DuplicateResourceException ex = assertThrows(DuplicateResourceException.class,
                () -> userService.register(request));
        assertEquals("该昵称已被使用", ex.getMessage());
    }

    // ===== Login Tests =====

    @Test
    void login_activeUser_returnsToken() {
        LoginRequest request = LoginRequest.builder()
                .username("testuser")
                .password("rawPassword")
                .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(activeUser));
        lenient().when(passwordEncoder.matches("rawPassword", "encodedPassword")).thenReturn(true);
        lenient().when(userRepository.save(any(User.class))).thenReturn(activeUser);
        lenient().when(jwtUtil.generateAccessToken(1L, "testuser")).thenReturn("access-token");
        lenient().when(jwtUtil.generateRefreshToken(1L, "testuser")).thenReturn("refresh-token");

        LoginResponse response = userService.login(request);

        assertEquals("access-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());
        assertEquals("ACTIVE", response.getUser().getStatus());
        verify(userRepository).save(activeUser);
    }

    @Test
    void login_pendingUser_throwsForbidden() {
        LoginRequest request = LoginRequest.builder()
                .username("pendinguser")
                .password("rawPassword")
                .build();

        when(userRepository.findByUsername("pendinguser")).thenReturn(Optional.of(pendingUser));
        lenient().when(passwordEncoder.matches("rawPassword", "encodedPassword")).thenReturn(true);

        ForbiddenException ex = assertThrows(ForbiddenException.class,
                () -> userService.login(request));
        assertEquals("账号等待审批中", ex.getMessage());
    }

    @Test
    void login_rejectedUser_throwsForbidden() {
        LoginRequest request = LoginRequest.builder()
                .username("rejecteduser")
                .password("rawPassword")
                .build();

        when(userRepository.findByUsername("rejecteduser")).thenReturn(Optional.of(rejectedUser));
        lenient().when(passwordEncoder.matches("rawPassword", "encodedPassword")).thenReturn(true);

        ForbiddenException ex = assertThrows(ForbiddenException.class,
                () -> userService.login(request));
        assertEquals("注册申请已被拒绝", ex.getMessage());
    }

    @Test
    void login_disabledUser_throwsForbidden() {
        LoginRequest request = LoginRequest.builder()
                .username("disableduser")
                .password("rawPassword")
                .build();

        when(userRepository.findByUsername("disableduser")).thenReturn(Optional.of(disabledUser));
        lenient().when(passwordEncoder.matches("rawPassword", "encodedPassword")).thenReturn(true);

        ForbiddenException ex = assertThrows(ForbiddenException.class,
                () -> userService.login(request));
        assertEquals("账号已被禁用", ex.getMessage());
    }

    @Test
    void login_wrongPassword_throwsUnauthorized() {
        LoginRequest request = LoginRequest.builder()
                .username("testuser")
                .password("wrongPassword")
                .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(activeUser));
        lenient().when(passwordEncoder.matches("wrongPassword", "encodedPassword")).thenReturn(false);

        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> userService.login(request));
        assertEquals("用户名或密码错误", ex.getMessage());
    }

    // ===== Admin Method Tests =====

    @Test
    void approveUser_pendingToActive() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(pendingUser));
        lenient().when(userRepository.save(any(User.class))).thenReturn(pendingUser);

        ApprovalResponse response = userService.approveUser(2L);

        assertEquals(2L, response.getUserId());
        assertEquals("ACTIVE", response.getStatus());
        assertEquals("审批通过", response.getMessage());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals(AccountStatus.ACTIVE, captor.getValue().getStatus());
    }

    @Test
    void approveUser_nonPending_throwsException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userService.approveUser(1L));
        assertEquals("该用户不在待审批状态", ex.getMessage());
    }

    @Test
    void rejectUser_pendingToRejected() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(pendingUser));
        lenient().when(userRepository.save(any(User.class))).thenReturn(pendingUser);

        ApprovalResponse response = userService.rejectUser(2L);

        assertEquals(2L, response.getUserId());
        assertEquals("REJECTED", response.getStatus());
        assertEquals("已拒绝", response.getMessage());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals(AccountStatus.REJECTED, captor.getValue().getStatus());
    }

    @Test
    void updateUserStatus_banUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
        lenient().when(userRepository.save(any(User.class))).thenReturn(activeUser);

        userService.updateUserStatus(1L, AccountStatus.DISABLED);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals(AccountStatus.DISABLED, captor.getValue().getStatus());
    }

    @Test
    void updateUserStatus_superAdmin_throwsException() {
        when(userRepository.findById(5L)).thenReturn(Optional.of(superAdminUser));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userService.updateUserStatus(5L, AccountStatus.DISABLED));
        assertEquals("不可操作超级管理员", ex.getMessage());
    }

    @Test
    void deleteUser_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));

        userService.deleteUser(1L);

        verify(userRepository).delete(activeUser);
    }

    @Test
    void deleteUser_superAdmin_throwsException() {
        when(userRepository.findById(5L)).thenReturn(Optional.of(superAdminUser));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userService.deleteUser(5L));
        assertEquals("不可删除超级管理员", ex.getMessage());
    }
}
