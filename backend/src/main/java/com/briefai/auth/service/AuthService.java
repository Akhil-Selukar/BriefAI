package com.briefai.auth.service;

import com.briefai.auth.controller.AuthController;
import com.briefai.auth.dto.NewUserRequest;
import com.briefai.auth.dto.NewUserResponse;
import com.briefai.exception.UserAlreadyExistsException;
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

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
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
        return new NewUserResponse(savedUser.getId(), savedUser.getName(), savedUser.getEmail(), savedUser.isEmailVerified());
    }
}
