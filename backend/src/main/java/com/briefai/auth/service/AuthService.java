package com.briefai.auth.service;

import com.briefai.auth.dto.*;
import com.briefai.email.service.EmailService;
import com.briefai.exception.auth.EmailNotVerifiedException;
import com.briefai.exception.auth.InvalidCredentialsException;
import com.briefai.exception.otpExceptions.EmailAlreadyVerifiedException;
import com.briefai.exception.otpExceptions.InvalidOtpException;
import com.briefai.exception.otpExceptions.OtpAttemptsExceededException;
import com.briefai.exception.otpExceptions.OtpExpiredException;
import com.briefai.exception.user.UserAlreadyExistsException;
import com.briefai.exception.user.UserNotFoundException;
import com.briefai.security.service.JwtService;
import com.briefai.user.entity.User;
import com.briefai.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
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
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, OtpService otpService, EmailService emailService,
                       AuthenticationManager authenticationManager, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.otpService = otpService;
        this.emailService = emailService;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public NewUserResponse register(NewUserRequest request) {
        logger.debug("Request to create new user received for {}", request.getName());
        String sanitizedEmail = request.getEmail().trim().toLowerCase();

        // Ensure that the user with same email id does not exist.
        if (userRepository.existsByEmail(sanitizedEmail)) {
            logger.warn("User with email {} already exists.", request.getEmail());
            throw new UserAlreadyExistsException();
        }

        String passwordHash = passwordEncoder.encode(request.getPassword());
        User user = new User(request.getName(), sanitizedEmail, passwordHash);
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
        logger.debug("Verifying account for email {}",sanitizedEmail);

        User user = userRepository.findByEmail(sanitizedEmail)
                .orElseThrow(() -> new UserNotFoundException("User not found."));

        if (user.isEmailVerified()) {
            logger.warn("The account is already verified.");
            throw new EmailAlreadyVerifiedException("Email is already verified.");
        }

        otpService.verifyOtp(user, request.getOtp());
        logger.debug("Verification successful.");
        user.markEmailAsVerified();
    }

    @Transactional
    public void resendOtp(ResendOtpRequest request) {
        String sanitizedEmail = request.getEmail().trim().toLowerCase();
        logger.debug("Resending verification OTP for {}",sanitizedEmail);

        User user = userRepository.findByEmail(sanitizedEmail).orElseThrow(() -> new UserNotFoundException("User not found."));

        if (user.isEmailVerified()) {
            logger.warn("This account is already verified.");
            throw new EmailAlreadyVerifiedException("Email is already verified.");
        }

        String otp = otpService.resendOtp(user);
        emailService.sendVerificationOtp(user.getEmail(), user.getName(), otp);
    }

    public LoginResponse login(LoginRequest request) {
        String sanitizedEmail = request.getEmail().trim().toLowerCase();
        logger.debug("Logging in to account {}",sanitizedEmail);
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(sanitizedEmail, request.getPassword()));
        } catch (AuthenticationException e) {
            logger.warn("Login failed : Invalid email or password");
            throw new InvalidCredentialsException("Invalid email or password.");
        }

        User user = userRepository.findByEmail(sanitizedEmail)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password."));

        if (!user.isEmailVerified()) {
            logger.warn("This account is not yet verified.");
            throw new EmailNotVerifiedException("Email must be verified before login.");
        }

        String accessToken = jwtService.generateToken(user);

        return new LoginResponse(accessToken, "Bearer", jwtService.getExpirationSeconds());
    }
}
