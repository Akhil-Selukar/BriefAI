package com.briefai.auth.service;

import com.briefai.auth.dto.NewUserRequest;
import com.briefai.auth.dto.NewUserResponse;
import com.briefai.exception.UserAlreadyExistsException;
import com.briefai.user.entity.User;
import com.briefai.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder);
    }

    @Test
    void register_shouldCreateUser() {
        NewUserRequest request = new NewUserRequest("Sheldon Cooper", "sheldon@test.com", "Sheldon#123");

        when(userRepository.existsByEmail("sheldon@test.com")).thenReturn(false);
        when(passwordEncoder.encode("Sheldon#123")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NewUserResponse response = authService.register(request);

        assertEquals("Sheldon Cooper", response.getName());
        assertEquals("sheldon@test.com", response.getEmail());
        assertFalse(response.isEmailVerified());

        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_shouldEncodePassword() {
        NewUserRequest request = new NewUserRequest("Penny", "Penny@test.com", "Penny#123");

        when(userRepository.existsByEmail("penny@test.com")).thenReturn(false);
        when(passwordEncoder.encode("Penny#123")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        authService.register(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();

        assertEquals("encoded-password", savedUser.getPassword());

        verify(passwordEncoder).encode("Penny#123");
    }

    @Test
    void register_shouldRejectDuplicateEmail() {
        NewUserRequest request = new NewUserRequest("Sheldon Cooper", "sheldon@test.com", "Sheldon#123");

        when(userRepository.existsByEmail("sheldon@test.com")).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> authService.register(request));

        verify(userRepository, never()).save(any(User.class));

        verify(passwordEncoder, never()).encode(anyString());
    }
}