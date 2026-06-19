package com.iaihub.toolbox.config;

import com.iaihub.toolbox.model.AccountStatus;
import com.iaihub.toolbox.model.Role;
import com.iaihub.toolbox.model.User;
import com.iaihub.toolbox.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.super-admin.username:admin}")
    private String superAdminUsername;

    @Value("${app.super-admin.password:Cloud@1234}")
    private String superAdminPassword;

    @Override
    public void run(String... args) {
        var existingOpt = userRepository.findByUsername(superAdminUsername);
        if (existingOpt.isPresent()) {
            var existing = existingOpt.get();
            if (existing.getRole() != Role.SUPER_ADMIN) {
                log.warn("User '{}' exists but has role {} (expected SUPER_ADMIN). Please fix manually.",
                        superAdminUsername, existing.getRole());
            } else {
                log.info("Super admin '{}' already exists, skipping initialization", superAdminUsername);
            }
            return;
        }

        User superAdmin = User.builder()
                .username(superAdminUsername)
                .nickname("超级管理员")
                .password(passwordEncoder.encode(superAdminPassword))
                .role(Role.SUPER_ADMIN)
                .status(AccountStatus.ACTIVE)
                .build();

        userRepository.save(superAdmin);
        log.info("Super admin '{}' created successfully", superAdminUsername);
    }
}
