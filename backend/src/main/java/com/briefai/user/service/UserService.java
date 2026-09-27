package com.briefai.user.service;

import com.briefai.exception.user.UserNotFoundException;
import com.briefai.user.dto.CurrentUserResponse;
import com.briefai.user.entity.User;
import com.briefai.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public CurrentUserResponse getCurrentUser(Long userId) {
        logger.debug("Fetching basic details for user {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found."));

        return new CurrentUserResponse(user.getId(), user.getName(), user.getEmail());
    }
}
