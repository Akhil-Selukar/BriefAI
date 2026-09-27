package com.briefai.user.service;

import com.briefai.exception.user.UserNotFoundException;
import com.briefai.user.dto.CurrentUserResponse;
import com.briefai.user.entity.User;
import com.briefai.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository);
    }

    @Test
    void getCurrentUser_shouldReturnCurrentUser() throws Exception {
        User user = new User("Penny", "penny@test.com", "1@ASfdfnbjbnBFs");
        ReflectionTestUtils.setField(user, "id", 1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        CurrentUserResponse response = userService.getCurrentUser(1L);

        assertEquals(1L, response.getId());
        assertEquals("Penny", response.getName());
        assertEquals("penny@test.com", response.getEmail());
    }


    @Test
    void getCurrentUser_shouldThrowUserNotFoundExceptionWhenUserDoesNotExist() throws Exception {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.getCurrentUser(1L));
    }
}