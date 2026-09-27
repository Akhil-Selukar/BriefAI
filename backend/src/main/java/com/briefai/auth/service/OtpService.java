package com.briefai.auth.service;

import com.briefai.auth.entity.EmailVerificationOtp;
import com.briefai.auth.repository.EmailVerificationOtpRepository;
import com.briefai.exception.otpExceptions.InvalidOtpException;
import com.briefai.exception.otpExceptions.OtpAttemptsExceededException;
import com.briefai.exception.otpExceptions.OtpExpiredException;
import com.briefai.exception.otpExceptions.OtpNotFoundException;
import com.briefai.user.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class OtpService {
    private static final Logger logger = LoggerFactory.getLogger(OtpService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final EmailVerificationOtpRepository otpRepository;
    private final PasswordEncoder passwordEncoder;
    private long expirationMinutes;
    private int maxAttempts;

    public OtpService(EmailVerificationOtpRepository otpRepository, PasswordEncoder passwordEncoder,
                      @Value("${app.otp.expiration-minutes}") long expirationMinutes, @Value("${app.otp.max-attempts}")int maxAttempts) {
        this.otpRepository = otpRepository;
        this.passwordEncoder = passwordEncoder;
        this.expirationMinutes = expirationMinutes;
        this.maxAttempts = maxAttempts;
    }

    @Transactional
    public String createOtp(User user) {
        logger.debug("Generating otp for user {}", user.getName());
        // Invalidate (mark as used) for all OTPS which are active in db for this user
        List<EmailVerificationOtp> existingOtps = otpRepository.findAllByUserIdAndUsedFalse(user.getId());
        existingOtps.forEach(EmailVerificationOtp::markAsUsed);

        String otp = generateOtp();
        String otpHash = passwordEncoder.encode(otp);

        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(expirationMinutes);

        EmailVerificationOtp verificationOtp = new EmailVerificationOtp(user, otpHash, expiresAt);
        otpRepository.save(verificationOtp);

        return otp;
    }

    private String generateOtp() {
        int number = SECURE_RANDOM.nextInt(1000000);    // generate secured random number between 0 to 999999
        
        return String.format("%06d", number);   // make it strictly 6 digit, add padding of 0 to the left if necessary.
    }

    @Transactional(noRollbackFor = {InvalidOtpException.class, OtpAttemptsExceededException.class, OtpExpiredException.class})
    public void verifyOtp(User user, String providedOtp) {

        // No OTP in DB condition
        EmailVerificationOtp verificationOtp = otpRepository.findFirstByUserIdAndUsedFalseOrderByCreatedAtDesc(user.getId())
                .orElseThrow(() -> new OtpNotFoundException("No active OTP found."));

        // Expired OTP condition
        if(verificationOtp.getExpiresAt().isBefore(LocalDateTime.now())) {
            verificationOtp.markAsUsed();
            throw new OtpExpiredException("OTP has expired.");
        }

        // Max OTP attempt exceeded condition
        if (verificationOtp.getAttemptCount() >= maxAttempts) {
            verificationOtp.markAsUsed();
            throw new OtpAttemptsExceededException("Maximum OTP verification attempts exceeded.");
        }

        boolean matches = passwordEncoder.matches(providedOtp, verificationOtp.getOtpHash());

        if (!matches) {
            verificationOtp.incrementAttemptCount();
            if (verificationOtp.getAttemptCount() >= maxAttempts) {
                // if final attempt is also incorrect then mark final OTP as invalid first
                // so that no additional attempt is allowed

                verificationOtp.markAsUsed();
                throw new OtpAttemptsExceededException("Maximum OTP verification attempts exceeded.");
            }
            throw new InvalidOtpException("Invalid OTP.");
        }

        // Success condition (OTP validation successful)
        verificationOtp.markAsUsed();
    }
}
