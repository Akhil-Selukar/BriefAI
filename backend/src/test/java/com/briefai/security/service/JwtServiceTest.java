package com.briefai.security.service;

import com.briefai.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private static final String SECRET = "VGhpc0lzQVN1ZmZpY2llbnRseUxvbmdUZXN0U2VjcmV0S2V5MTIzNDU2";
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-28T00:00:00Z"), ZoneOffset.UTC);

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, 60, clock);
    }

    private User createVerifiedUser() {
        User user = new User("Penny", "penny@test.com", "Penny#123");

        ReflectionTestUtils.setField(user, "id", 1L);
        user.markEmailAsVerified();

        return user;
    }

    @Test
    void extractUserId_shouldReturnUserIdFromGeneratedToken() {
        User user = createVerifiedUser();

        String token = jwtService.generateToken(user);
        Long userId = jwtService.extractUserId(token);

        assertEquals(1L, userId);
    }

    @Test
    void isTokenValid_shouldReturnTrueForValidToken() {
        User user = createVerifiedUser();

        String token = jwtService.generateToken(user);
        boolean valid = jwtService.isTokenValid(token);

        assertTrue(valid);
    }

    @Test
    void isTokenValid_shouldReturnFalseForTamperedToken() {

        User user = createVerifiedUser();

        String token = jwtService.generateToken(user);
        String tamperedToken = token.replace('e', 'E');

        boolean valid = jwtService.isTokenValid(tamperedToken);

        assertFalse(valid);
    }
}