package com.briefai.auth.service;

import com.briefai.auth.dto.NewUserRequest;
import com.briefai.auth.dto.NewUserResponse;
import com.briefai.auth.dto.VerifyEmailRequest;
import com.briefai.email.EmailService;
import com.briefai.exception.otpExceptions.EmailAlreadyVerifiedException;
import com.briefai.exception.otpExceptions.InvalidOtpException;
import com.briefai.exception.otpExceptions.OtpAttemptsExceededException;
import com.briefai.exception.otpExceptions.OtpExpiredException;
import com.briefai.exception.user.UserAlreadyExistsException;
import com.briefai.exception.user.UserNotFoundException;
import com.briefai.user.entity.User;
import com.briefai.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;
    private final EmailService emailService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, OtpService otpService, EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.otpService = otpService;
        this.emailService = emailService;
    }

    @Transactional
    public NewUserResponse register(NewUserRequest request) {
        logger.debug("Request to create new user received for {}", request.getName());
        String email = request.getEmail().trim().toLowerCase();

        // Ensure that the user with same email id does not exist.
        if (userRepository.existsByEmail(email)) {
            logger.warn("User with email {} already exists.",request.getEmail());
            throw new UserAlreadyExistsException();
        }

        String passwordHash = passwordEncoder.encode(request.getPassword());
        User user = new User(request.getName(), email, passwordHash);
        User savedUser = userRepository.save(user);
        logger.debug("User {} created successfully.", savedUser.getName());

        // generate OTP for the user
        String otp = otpService.createOtp(savedUser);

        // Send email to user
        emailService.sendVerificationOtp(savedUser.getEmail(), savedUser.getName(), otp);

        return new NewUserResponse(savedUser.getId(), savedUser.getName(), savedUser.getEmail(), savedUser.isEmailVerified());
    }

    @Transactional(noRollbackFor = {InvalidOtpException.class, OtpAttemptsExceededException.class, OtpExpiredException.class})
    public void verifyEmail(VerifyEmailRequest request) {

        String sanitizedEmail = request.getEmail().trim().toLowerCase();

        User user = userRepository.findByEmail(sanitizedEmail)
                .orElseThrow(() -> new UserNotFoundException("User not found."));

        if (user.isEmailVerified()) {
            throw new EmailAlreadyVerifiedException("Email is already verified.");
        }

        otpService.verifyOtp(user, request.getOtp());

        user.markEmailAsVerified();
    }
}
