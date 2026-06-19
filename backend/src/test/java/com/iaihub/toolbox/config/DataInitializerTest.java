package com.iaihub.toolbox.config;

import com.iaihub.toolbox.model.AccountStatus;
import com.iaihub.toolbox.model.Role;
import com.iaihub.toolbox.model.User;
import com.iaihub.toolbox.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataInitializerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private DataInitializer dataInitializer;

    @BeforeEach
    void setUp() {
        dataInitializer = new DataInitializer(userRepository, passwordEncoder);
        ReflectionTestUtils.setField(dataInitializer, "superAdminUsername", "admin");
        ReflectionTestUtils.setField(dataInitializer, "superAdminPassword", "Cloud@1234");
    }

    @Test
    void run_createsSuperAdminWhenNotExists() throws Exception {
        // Given
        when(userRepository.findByUsername("admin")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("Cloud@1234")).thenReturn("$2a$encoded_password");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        dataInitializer.run();

        // Then
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();

        assertEquals("admin", saved.getUsername());
        assertEquals("$2a$encoded_password", saved.getPassword());
        assertEquals(Role.SUPER_ADMIN, saved.getRole());
        assertEquals(AccountStatus.ACTIVE, saved.getStatus());
    }

    @Test
    void run_skipsWhenSuperAdminAlreadyExists() throws Exception {
        // Given
        User existing = User.builder()
                .username("admin")
                .role(Role.SUPER_ADMIN)
                .status(AccountStatus.ACTIVE)
                .build();
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(existing));

        // When
        dataInitializer.run();

        // Then
        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void run_warnsWhenAdminUserExistsButNotSuperAdmin() throws Exception {
        // Given - a regular user named 'admin' exists
        User existing = User.builder()
                .username("admin")
                .role(Role.USER)
                .status(AccountStatus.ACTIVE)
                .build();
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(existing));

        // When
        dataInitializer.run();

        // Then - should not create a new super admin (avoid overwriting)
        verify(userRepository, never()).save(any());
    }

    @Test
    void run_encodesPasswordWithBCrypt() throws Exception {
        // Given
        when(userRepository.findByUsername("admin")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("Cloud@1234")).thenReturn("$2a$10$hash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        dataInitializer.run();

        // Then
        verify(passwordEncoder).encode("Cloud@1234");
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals("$2a$10$hash", captor.getValue().getPassword());
    }
}
