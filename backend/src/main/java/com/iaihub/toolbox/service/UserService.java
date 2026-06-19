package com.iaihub.toolbox.service;

import com.iaihub.toolbox.config.UploadConfig;
import com.iaihub.toolbox.dto.*;
import com.iaihub.toolbox.exception.AvatarValidationException;
import com.iaihub.toolbox.exception.DuplicateResourceException;
import com.iaihub.toolbox.exception.ForbiddenException;
import com.iaihub.toolbox.exception.UnauthorizedException;
import com.iaihub.toolbox.exception.UserNotFoundException;
import com.iaihub.toolbox.model.AccountStatus;
import com.iaihub.toolbox.model.Role;
import com.iaihub.toolbox.model.User;
import com.iaihub.toolbox.repository.UserRepository;
import com.iaihub.toolbox.util.AvatarUtil;
import com.iaihub.toolbox.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private static final Pattern SIZE_PATTERN = Pattern.compile("(\\d+)\\s*(B|KB|MB|GB)?", Pattern.CASE_INSENSITIVE);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final UploadConfig uploadConfig;

    @Transactional
    public LoginResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("该用户名已被注册");
        }
        if (userRepository.existsByNickname(request.getNickname())) {
            throw new DuplicateResourceException("该昵称已被使用");
        }

        // Parse and validate role
        Role role = Role.USER;
        if (request.getRole() != null && !request.getRole().isBlank()) {
            try {
                role = Role.valueOf(request.getRole().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("无效的角色类型");
            }
            if (role == Role.SUPER_ADMIN) {
                throw new IllegalArgumentException("不允许注册超级管理员");
            }
        }

        // Set status based on role
        AccountStatus status = (role == Role.ADMIN) ? AccountStatus.PENDING : AccountStatus.ACTIVE;

        User user = User.builder()
                .username(request.getUsername())
                .nickname(request.getNickname())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .status(status)
                .build();

        user = userRepository.save(user);

        // ADMIN registration: no token, return pending message
        if (role == Role.ADMIN) {
            return LoginResponse.builder()
                    .user(LoginResponse.UserDTO.builder()
                            .id(user.getId())
                            .username(user.getUsername())
                            .nickname(user.getNickname())
                            .avatarUrl(user.getAvatarUrl())
                            .role(user.getRole().name())
                            .status(user.getStatus().name())
                            .build())
                    .build();
        }

        // USER registration: return tokens
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getUsername());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(LoginResponse.UserDTO.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .nickname(user.getNickname())
                        .avatarUrl(user.getAvatarUrl())
                        .role(user.getRole().name())
                        .status(user.getStatus().name())
                        .build())
                .build();
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new UnauthorizedException("用户名或密码错误"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new UnauthorizedException("用户名或密码错误");
        }

        // Check account status
        switch (user.getStatus()) {
            case PENDING:
                throw new ForbiddenException("账号等待审批中");
            case REJECTED:
                throw new ForbiddenException("注册申请已被拒绝");
            case DISABLED:
                throw new ForbiddenException("账号已被禁用");
            case ACTIVE:
            default:
                break;
        }

        // Update last login time
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getUsername());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(LoginResponse.UserDTO.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .nickname(user.getNickname())
                        .avatarUrl(user.getAvatarUrl())
                        .role(user.getRole().name())
                        .status(user.getStatus().name())
                        .build())
                .build();
    }

    public RefreshResponse refreshToken(String refreshToken) {
        if (!jwtUtil.validateToken(refreshToken) || !jwtUtil.isRefreshToken(refreshToken)) {
            throw new UnauthorizedException("无效的刷新令牌");
        }

        Long userId = jwtUtil.getUserIdFromToken(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("用户不存在"));

        String newAccessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername());

        return RefreshResponse.builder()
                .accessToken(newAccessToken)
                .build();
    }

    public UserDTO getCurrentUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("用户不存在"));

        return UserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole().name())
                .status(user.getStatus().name())
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }

    @Transactional
    public AvatarUploadResponse uploadAvatar(Long userId, MultipartFile file) {
        // 1. 校验
        String ext = AvatarUtil.validateAndGetExtension(file);
        long maxBytes = parseSizeToBytes(uploadConfig.getAvatarMaxFileSize());
        if (file.getSize() > maxBytes) {
            throw new AvatarValidationException("头像文件不能超过 " + uploadConfig.getAvatarMaxFileSize());
        }

        // 2. 找 user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        // 3. 准备目录
        Path avatarDir = Paths.get(uploadConfig.getBaseDir(), uploadConfig.getAvatarSubdir());
        try {
            Files.createDirectories(avatarDir);
        } catch (IOException e) {
            log.error("无法创建头像目录: {}", avatarDir, e);
            throw new RuntimeException("无法创建头像目录", e);
        }

        // 4. 删旧
        deleteExistingAvatars(avatarDir, userId);

        // 5. 写新
        String normalizedExt = AvatarUtil.normalizeExt(ext);
        Path target = avatarDir.resolve(userId + "." + normalizedExt);
        try {
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("写入头像失败: {}", target, e);
            throw new RuntimeException("写入头像失败", e);
        }

        // 6. 更新 user
        user.setAvatarUrl("/api/v1/static/avatars/" + userId + "." + normalizedExt);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        // 7. 构造响应
        long timestamp = user.getUpdatedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        return AvatarUploadResponse.builder()
                .avatarUrl(user.getAvatarUrl() + "?v=" + timestamp)
                .fileSize(file.getSize())
                .uploadedAt(user.getUpdatedAt())
                .build();
    }

    @Transactional
    public void deleteAvatar(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (user.getAvatarUrl() != null) {
            Path avatarDir = Paths.get(uploadConfig.getBaseDir(), uploadConfig.getAvatarSubdir());
            deleteExistingAvatars(avatarDir, userId);
        }

        user.setAvatarUrl(null);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    public PublicUserDTO getPublicProfile(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        return PublicUserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .createdAt(user.getCreatedAt())
                .build();
    }

    private void deleteExistingAvatars(Path dir, Long userId) {
        if (!Files.exists(dir)) return;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, userId + ".*")) {
            for (Path p : stream) {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException e) {
                    log.warn("无法删除旧头像 {}: {}", p, e.getMessage());
                }
            }
        } catch (IOException e) {
            log.warn("无法列举头像目录 {}: {}", dir, e.getMessage());
        }
    }

    private long parseSizeToBytes(String sizeStr) {
        if (sizeStr == null || sizeStr.isBlank()) return 2L * 1024 * 1024;
        var matcher = SIZE_PATTERN.matcher(sizeStr.trim().toUpperCase());
        if (!matcher.matches()) return 2L * 1024 * 1024;
        long n = Long.parseLong(matcher.group(1));
        String unit = matcher.group(2);
        if (unit == null || "B".equals(unit)) return n;
        if ("KB".equals(unit)) return n * 1024L;
        if ("MB".equals(unit)) return n * 1024L * 1024L;
        if ("GB".equals(unit)) return n * 1024L * 1024L * 1024L;
        return n;
    }

    // ===== Admin Methods =====

    public List<PendingUserDTO> getPendingUsers() {
        return userRepository.findByStatusAndRole(AccountStatus.PENDING, Role.ADMIN).stream()
                .map(u -> PendingUserDTO.builder()
                        .id(u.getId())
                        .username(u.getUsername())
                        .nickname(u.getNickname())
                        .role(u.getRole().name())
                        .createdAt(u.getCreatedAt())
                        .build())
                .toList();
    }

    @Transactional
    public ApprovalResponse approveUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (user.getStatus() != AccountStatus.PENDING) {
            throw new IllegalArgumentException("该用户不在待审批状态");
        }

        user.setStatus(AccountStatus.ACTIVE);
        userRepository.save(user);

        return ApprovalResponse.builder()
                .userId(user.getId())
                .status(AccountStatus.ACTIVE.name())
                .message("审批通过")
                .build();
    }

    @Transactional
    public ApprovalResponse rejectUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (user.getStatus() != AccountStatus.PENDING) {
            throw new IllegalArgumentException("该用户不在待审批状态");
        }

        user.setStatus(AccountStatus.REJECTED);
        userRepository.save(user);

        return ApprovalResponse.builder()
                .userId(user.getId())
                .status(AccountStatus.REJECTED.name())
                .message("已拒绝")
                .build();
    }

    public Page<AdminUserDTO> getUsers(Role role, AccountStatus status, String keyword, Pageable pageable) {
        return userRepository.findAllFiltered(role, status, keyword, pageable)
                .map(u -> AdminUserDTO.builder()
                        .id(u.getId())
                        .username(u.getUsername())
                        .nickname(u.getNickname())
                        .role(u.getRole().name())
                        .status(u.getStatus().name())
                        .createdAt(u.getCreatedAt())
                        .lastLoginAt(u.getLastLoginAt())
                        .build());
    }

    @Transactional
    public void updateUserStatus(Long userId, AccountStatus newStatus) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (user.getRole() == Role.SUPER_ADMIN) {
            throw new IllegalArgumentException("不可操作超级管理员");
        }

        user.setStatus(newStatus);
        userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (user.getRole() == Role.SUPER_ADMIN) {
            throw new IllegalArgumentException("不可删除超级管理员");
        }

        userRepository.delete(user);
    }
}
