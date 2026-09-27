package com.briefai.auth.service;

import com.briefai.auth.dto.*;
import com.briefai.email.service.EmailService;
import com.briefai.exception.auth.EmailNotVerifiedException;
import com.briefai.exception.auth.InvalidCredentialsException;
import com.briefai.exception.otpExceptions.EmailAlreadyVerifiedException;
import com.briefai.exception.otpExceptions.InvalidOtpException;
import com.briefai.exception.user.UserAlreadyExistsException;
import com.briefai.exception.user.UserNotFoundException;
import com.briefai.security.service.JwtService;
import com.briefai.user.entity.User;
import com.briefai.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private OtpService otpService;

    @Mock
    private EmailService emailService;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtService jwtService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, otpService, emailService, authenticationManager, jwtService);
    }

    @Test
    void register_shouldCreateUser() {
        NewUserRequest request = new NewUserRequest("Sheldon Cooper", "sheldon@test.com", "Sheldon#123");

        when(userRepository.existsByEmail("sheldon@test.com")).thenReturn(false);
        when(passwordEncoder.encode("Sheldon#123")).thenReturn("1@asdDFcdBjlipe");
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
        when(passwordEncoder.encode("Penny#123")).thenReturn("1@asdDFcdBjlipe");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        authService.register(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();

        assertEquals("1@asdDFcdBjlipe", savedUser.getPassword());

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

    @Test
    void register_shouldCreateVerificationOtp() {

        NewUserRequest request = new NewUserRequest("Penny", "penny@test.com", "Penny#123");

        when(userRepository.existsByEmail("penny@test.com")).thenReturn(false);
        when(passwordEncoder.encode("Penny#123")).thenReturn("1@asdDFcdBjlipe");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(otpService.createOtp(any(User.class))).thenReturn("123456");

        authService.register(request);
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        verify(userRepository).save(userCaptor.capture());
        verify(otpService).createOtp(userCaptor.getValue());
    }

    @Test
    void verifyEmail_shouldVerifyUserWhenOtpIsValid() {

        VerifyEmailRequest request = new VerifyEmailRequest("penny@test.com", "123456");
        User user = new User("Penny", "penny@test.com", "1@asdDFcdBjlipe");

        when(userRepository.findByEmail("penny@test.com")).thenReturn(Optional.of(user));

        authService.verifyEmail(request);

        verify(userRepository).findByEmail("penny@test.com");
        verify(otpService).verifyOtp(user, "123456");

        assertTrue(user.isEmailVerified());
    }

    @Test
    void verifyEmail_shouldThrowWhenUserDoesNotExist() {
        VerifyEmailRequest request = new VerifyEmailRequest("penny@test.com", "123456");

        when(userRepository.findByEmail("penny@test.com")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> authService.verifyEmail(request));
        verifyNoInteractions(otpService);
    }

    @Test
    void verifyEmail_shouldRejectAlreadyVerifiedUser() {
        VerifyEmailRequest request = new VerifyEmailRequest("penny@test.com", "123456");
        User user = new User("Penny", "penny@test.com", "1@asdDFcdBjlipe");

        user.markEmailAsVerified();     // verified the user

        when(userRepository.findByEmail("penny@test.com")).thenReturn(Optional.of(user));

        assertThrows(EmailAlreadyVerifiedException.class, () -> authService.verifyEmail(request));
        verifyNoInteractions(otpService);
    }

    @Test
    void verifyEmail_shouldNotVerifyUserWhenOtpIsInvalid() {
        VerifyEmailRequest request = new VerifyEmailRequest("penny@test.com", "999999");

        User user = new User("Penny", "penny@test.com", "1@asdDFcdBjlipe");

        when(userRepository.findByEmail("penny@test.com")).thenReturn(Optional.of(user));
        doThrow(new InvalidOtpException("Invalid OTP.")).when(otpService).verifyOtp(user, "999999");

        assertThrows(InvalidOtpException.class, () -> authService.verifyEmail(request));
        assertFalse(user.isEmailVerified());
    }

    @Test
    void register_shouldSendVerificationOtpByEmail() {
        NewUserRequest request = new NewUserRequest("Penny", "Penny@test.com", "Penny#123");

        when(userRepository.existsByEmail("penny@test.com")).thenReturn(false);
        when(passwordEncoder.encode("Penny#123")).thenReturn("1@asdDFcdBjlipe");

        User savedUser = new User("Penny", "penny@test.com", "1@asdDFcdBjlipe");

        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(otpService.createOtp(savedUser)).thenReturn("123456");

        authService.register(request);

        verify(emailService).sendVerificationOtp("penny@test.com", "Penny", "123456");
    }

    @Test
    void resendOtp_shouldSendNewOtp() {
        ResendOtpRequest request = new ResendOtpRequest("penny@test.com");
        User user = new User("Penny", "penny@test.com", "1@asdDFcdBjlipe");

        when(userRepository.findByEmail("penny@test.com")).thenReturn(Optional.of(user));
        when(otpService.resendOtp(user)).thenReturn("654321");

        authService.resendOtp(request);

        verify(otpService).resendOtp(user);
        verify(emailService).sendVerificationOtp("penny@test.com", "Penny", "654321");
    }

    @Test
    void resendOtp_shouldThrowUserNotFoundWhenUserDoesNotExist() {

        ResendOtpRequest request = new ResendOtpRequest("sheldon@test.com");

        when(userRepository.findByEmail("sheldon@test.com")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> authService.resendOtp(request));

        verifyNoInteractions(otpService);
        verifyNoInteractions(emailService);
    }

    @Test
    void resendOtp_shouldThrowAlreadyVerifiedForAlreadyVerifiedUser() {

        ResendOtpRequest request = new ResendOtpRequest("penny@test.com");

        User user = new User("Penny", "penny@test.com", "1@asdDFcdBjlipe");
        user.markEmailAsVerified();

        when(userRepository.findByEmail("penny@test.com")).thenReturn(Optional.of(user));
        assertThrows(EmailAlreadyVerifiedException.class, () -> authService.resendOtp(request));

        verifyNoInteractions(otpService);
        verifyNoInteractions(emailService);
    }

    @Test
    void login_shouldSucceedForValidCredentials() {
        LoginRequest request = new LoginRequest("penny@test.com", "Penny#123");

        User user = new User("Penny", "penny@test.com", "1@asdDFcdBjlipe");
        user.markEmailAsVerified();

        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(mock(Authentication.class));
        when(userRepository.findByEmail("penny@test.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3O");
        when(jwtService.getExpirationSeconds()).thenReturn(3600L);

        assertDoesNotThrow(() -> authService.login(request));
    }

    @Test
    void login_shouldThrowInvalidCredentialsForWrongPassword() {
        LoginRequest request = new LoginRequest("penny@test.com", "Penny123457889");

        when(authenticationManager.authenticate(any(Authentication.class))).thenThrow(new BadCredentialsException("Bad credentials"));
        assertThrows(InvalidCredentialsException.class, () -> authService.login(request));
    }

    @Test
    void login_shouldThrowEmailNotVerifiedExceptionForUnverifiedEmail() {

        LoginRequest request = new LoginRequest("penny@test.com", "Penny#123");
        User user = new User("Penny", "penny@test.com", "1@asdDFcdBjlipe");

        when(userRepository.findByEmail("penny@test.com")).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(mock(Authentication.class));

        assertThrows(EmailNotVerifiedException.class, () -> authService.login(request));
        verifyNoInteractions(jwtService);
    }

    @Test
    void login_shouldInvalidCredentialsExceptionForUnregisteredEmail() {
        LoginRequest request = new LoginRequest("penny@test.com", "Penny#123");

        when(userRepository.findByEmail("penny@test.com")).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class, () -> authService.login(request));
    }
}