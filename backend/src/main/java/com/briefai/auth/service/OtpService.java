package com.briefai.auth.service;

import com.briefai.auth.entity.EmailVerificationOtp;
import com.briefai.auth.repository.EmailVerificationOtpRepository;
import com.briefai.exception.otpExceptions.*;
import com.briefai.user.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class OtpService {
    private static final Logger logger = LoggerFactory.getLogger(OtpService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final EmailVerificationOtpRepository otpRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final long expirationMinutes;
    private final int maxAttempts;
    private final long resendCoolDownSeconds;

    public OtpService(EmailVerificationOtpRepository otpRepository, PasswordEncoder passwordEncoder,
                      @Value("${app.otp.expiration-minutes}") long expirationMinutes, @Value("${app.otp.max-attempts}")int maxAttempts,
                    @Value("${app.otp.resend-cooldown-seconds}") long resendCoolDownSeconds, Clock clock) {
        this.otpRepository = otpRepository;
        this.passwordEncoder = passwordEncoder;
        this.expirationMinutes = expirationMinutes;
        this.maxAttempts = maxAttempts;
        this.resendCoolDownSeconds = resendCoolDownSeconds;
        this.clock = clock;
    }

    @Transactional
    public String createOtp(User user) {
        logger.debug("Generating otp for user {}", user.getName());
        // Invalidate (mark as used) for all OTPS which are active in db for this user
        List<EmailVerificationOtp> existingOtps = otpRepository.findAllByUserIdAndUsedFalse(user.getId());
        existingOtps.forEach(EmailVerificationOtp::markAsUsed);

        String otp = generateOtp();
        String otpHash = passwordEncoder.encode(otp);

        LocalDateTime createdAt = LocalDateTime.now(clock);
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(expirationMinutes);

        EmailVerificationOtp verificationOtp = new EmailVerificationOtp(user, otpHash, expiresAt, createdAt);
        otpRepository.save(verificationOtp);

        return otp;
    }

    @Transactional
    public String resendOtp(User user) {

        Optional<EmailVerificationOtp> latestOtp = otpRepository.findFirstByUserIdAndUsedFalseOrderByCreatedAtDesc(user.getId());

        // if an OTP generated withing cool down time exist then do not create new OTP
        if (latestOtp.isPresent()) {
            EmailVerificationOtp existingOtp = latestOtp.get();

            LocalDateTime coolDownEndsAt = existingOtp.getCreatedAt().plusSeconds(resendCoolDownSeconds);
            LocalDateTime currTime = LocalDateTime.now(clock);

            if (currTime.isBefore(coolDownEndsAt)) {
                throw new OtpResendCoolDownException("Please wait before requesting another OTP.");
            }
        }

        // else request new OTP, createOTP() handles the invalidation of old active OTPs present for the user.
        return createOtp(user);
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
