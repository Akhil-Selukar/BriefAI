package com.briefai.auth.service;

import com.briefai.auth.entity.EmailVerificationOtp;
import com.briefai.auth.repository.EmailVerificationOtpRepository;
import com.briefai.exception.otpExceptions.*;
import com.briefai.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

    @Mock
    private EmailVerificationOtpRepository otpRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private OtpService otpService;

    private User user;

    private Clock clock;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(Instant.parse("2026-09-27T10:00:00Z"), ZoneOffset.UTC);
        otpService = new OtpService(otpRepository, passwordEncoder, 5, 5, 60, clock);
        user = new User("Penny", "penny@test.com", "encoded-password");
    }

    @Test
    void createOtp_shouldGenerateSixDigitOtp() {
        when(otpRepository.findAllByUserIdAndUsedFalse(any())).thenReturn(List.of());
        when(passwordEncoder.encode(anyString())).thenReturn("2$HFfsdbmn2sf#23");

        String otp = otpService.createOtp(user);

        assertNotNull(otp);
        assertTrue(otp.matches("\\d{6}"));
    }

    @Test
    void createOtp_shouldHashOtpBeforeSaving() {

        when(otpRepository.findAllByUserIdAndUsedFalse(any())).thenReturn(List.of());
        when(passwordEncoder.encode(anyString())).thenReturn("2$HFfsdbmn2sf#23");

        String otp = otpService.createOtp(user);

        verify(passwordEncoder).encode(otp);

        ArgumentCaptor<EmailVerificationOtp> captor = ArgumentCaptor.forClass(EmailVerificationOtp.class);

        verify(otpRepository).save(captor.capture());

        EmailVerificationOtp savedOtp = captor.getValue();

        assertEquals("2$HFfsdbmn2sf#23", savedOtp.getOtpHash());
        assertNotEquals(otp, savedOtp.getOtpHash());
    }

    @Test
    void createOtp_shouldSetExpiration() {

        when(otpRepository.findAllByUserIdAndUsedFalse(any())).thenReturn(List.of());
        when(passwordEncoder.encode(anyString())).thenReturn("2$HFfsdbmn2sf#23");

        LocalDateTime before = LocalDateTime.now();

        otpService.createOtp(user);

        LocalDateTime after = LocalDateTime.now();

        ArgumentCaptor<EmailVerificationOtp> captor = ArgumentCaptor.forClass(EmailVerificationOtp.class);

        verify(otpRepository).save(captor.capture());

        LocalDateTime expiresAt = captor.getValue().getExpiresAt();

        assertFalse(expiresAt.isBefore(before.plusMinutes(5)));
        assertFalse(expiresAt.isAfter(after.plusMinutes(5)));
    }

    @Test
    void createOtp_shouldMarkExistingOtpsAsUsed() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 9, 27, 9, 59, 30);
        EmailVerificationOtp oldOtp1 = new EmailVerificationOtp(user, "2$HFfsdbmn2sf#23", createdAt.plusMinutes(2), createdAt);
        EmailVerificationOtp oldOtp2 = new EmailVerificationOtp(user, "1#asdasShD$ADa", createdAt.plusMinutes(3), createdAt);

        when(otpRepository.findAllByUserIdAndUsedFalse(any())).thenReturn(List.of(oldOtp1, oldOtp2));
        when(passwordEncoder.encode(anyString())).thenReturn("3Dfgfsdf$F%asd#");

        otpService.createOtp(user);

        assertTrue(oldOtp1.isUsed());
        assertTrue(oldOtp2.isUsed());

        verify(otpRepository).save(any(EmailVerificationOtp.class));
    }

    // verify OTP tests
    @Test
    void verifyOtp_shouldMarkOtpAsUsedWhenOtpIsCorrect() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 9, 27, 9, 59, 30);
        EmailVerificationOtp verificationOtp = new EmailVerificationOtp(user, "3Dfgfsdf$F%asd#", LocalDateTime.now().plusMinutes(5), createdAt);

        when(otpRepository.findFirstByUserIdAndUsedFalseOrderByCreatedAtDesc(isNull())).thenReturn(Optional.of(verificationOtp));
        when(passwordEncoder.matches("123456", "3Dfgfsdf$F%asd#")).thenReturn(true);

        otpService.verifyOtp(user, "123456");

        assertTrue(verificationOtp.isUsed());
        verify(passwordEncoder).matches("123456", "3Dfgfsdf$F%asd#");
    }

    @Test
    void verifyOtp_shouldIncrementAttemptCountWhenOtpIsWrong() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 9, 27, 9, 59, 30);
        EmailVerificationOtp verificationOtp = new EmailVerificationOtp(user, "3Dfgfsdf$F%asd#", LocalDateTime.now().plusMinutes(5), createdAt);

        when(otpRepository.findFirstByUserIdAndUsedFalseOrderByCreatedAtDesc(isNull())).thenReturn(Optional.of(verificationOtp));
        when(passwordEncoder.matches("999999", "3Dfgfsdf$F%asd#")).thenReturn(false);

        assertThrows(InvalidOtpException.class, () -> otpService.verifyOtp(user, "999999"));

        assertEquals(1, verificationOtp.getAttemptCount());
        assertFalse(verificationOtp.isUsed());
    }

    @Test
    void verifyOtp_shouldMarkOtpAsUsedOnFifthFailedAttempt() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 9, 27, 9, 59, 30);
        EmailVerificationOtp verificationOtp = new EmailVerificationOtp(user, "3Dfgfsdf$F%asd#", LocalDateTime.now().plusMinutes(5), createdAt);

        // simulate 4 already failed attempts
        verificationOtp.incrementAttemptCount();
        verificationOtp.incrementAttemptCount();
        verificationOtp.incrementAttemptCount();
        verificationOtp.incrementAttemptCount();

        assertEquals(4, verificationOtp.getAttemptCount());

        when(otpRepository.findFirstByUserIdAndUsedFalseOrderByCreatedAtDesc(isNull())).thenReturn(Optional.of(verificationOtp));
        when(passwordEncoder.matches("999999", "3Dfgfsdf$F%asd#")).thenReturn(false);

        assertThrows(OtpAttemptsExceededException.class, () -> otpService.verifyOtp(user, "999999"));
        assertEquals(5, verificationOtp.getAttemptCount());
        assertTrue(verificationOtp.isUsed());
    }

    @Test
    void verifyOtp_shouldRejectExpiredOtp() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 9, 27, 9, 59, 30);
        EmailVerificationOtp verificationOtp = new EmailVerificationOtp(user, "3Dfgfsdf$F%asd#", createdAt.minusMinutes(1), createdAt);

        when(otpRepository.findFirstByUserIdAndUsedFalseOrderByCreatedAtDesc(isNull())).thenReturn(Optional.of(verificationOtp));

        assertThrows(OtpExpiredException.class, () -> otpService.verifyOtp(user, "123456"));
        assertTrue(verificationOtp.isUsed());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void verifyOtp_shouldThrowWhenNoActiveOtpExists() {

        when(otpRepository.findFirstByUserIdAndUsedFalseOrderByCreatedAtDesc(isNull())).thenReturn(Optional.empty());

        assertThrows(OtpNotFoundException.class, () -> otpService.verifyOtp(user, "123456"));

        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void verifyOtp_shouldRejectOtpWhenAttemptsAlreadyExceeded() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 9, 27, 9, 59, 30);
        EmailVerificationOtp verificationOtp = new EmailVerificationOtp(user, "3Dfgfsdf$F%asd#", LocalDateTime.now().plusMinutes(5), createdAt);

        // All 5 attempts are already exhausted
        verificationOtp.incrementAttemptCount();
        verificationOtp.incrementAttemptCount();
        verificationOtp.incrementAttemptCount();
        verificationOtp.incrementAttemptCount();
        verificationOtp.incrementAttemptCount();

        when(otpRepository.findFirstByUserIdAndUsedFalseOrderByCreatedAtDesc(isNull())).thenReturn(Optional.of(verificationOtp));

        assertThrows(OtpAttemptsExceededException.class, () -> otpService.verifyOtp(user, "123456"));
        assertTrue(verificationOtp.isUsed());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void resendOtp_shouldRejectRequestDuringCoolDown() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 9, 27, 9, 59, 30);

        EmailVerificationOtp existingOtp = new EmailVerificationOtp(user, "3Dfgfsdf$F%asd#", createdAt.plusMinutes(5), createdAt);
        when(otpRepository.findFirstByUserIdAndUsedFalseOrderByCreatedAtDesc(isNull())).thenReturn(Optional.of(existingOtp));

        assertThrows(OtpResendCoolDownException.class, () -> otpService.resendOtp(user));
        assertFalse(existingOtp.isUsed());
    }

    @Test
    void resendOtp_shouldCreateNewOtpAfterCoolDown() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 9, 27, 9, 58, 30);
        EmailVerificationOtp existingOtp = new EmailVerificationOtp(user, "3Dfgfsdf$F%asd#", createdAt.plusMinutes(5), createdAt);

        when(otpRepository.findFirstByUserIdAndUsedFalseOrderByCreatedAtDesc(isNull())).thenReturn(Optional.of(existingOtp));
        when(otpRepository.findAllByUserIdAndUsedFalse(isNull())).thenReturn(List.of(existingOtp));
        when(passwordEncoder.encode(anyString())).thenReturn("4fHasdll7D&Sada");

        String newOtp = otpService.resendOtp(user);

        assertTrue(existingOtp.isUsed());
        assertTrue(newOtp.matches("\\d{6}"));
        verify(otpRepository).save(any(EmailVerificationOtp.class));
    }
}