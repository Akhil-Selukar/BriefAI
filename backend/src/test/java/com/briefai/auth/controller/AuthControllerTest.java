package com.briefai.auth.controller;

import com.briefai.auth.dto.NewUserRequest;
import com.briefai.auth.dto.NewUserResponse;
import com.briefai.auth.service.AuthService;
import com.briefai.exception.UserAlreadyExistsException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void register_shouldReturn201ForValidRequest() throws Exception {

        NewUserRequest request = new NewUserRequest("Penny", "penny@test.com", "Penny#123");

        NewUserResponse response = new NewUserResponse(1L, "Penny", "penny@test.com", false);

        when(authService.register(any(NewUserRequest.class))).thenReturn(response);

        mockMvc.perform(
                post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Penny"))
                .andExpect(jsonPath("$.email").value("penny@test.com"))
                .andExpect(jsonPath("$.emailVerified").value(false));
    }

    @Test
    void register_shouldReturn400WhenEmailMissing() throws Exception {

        String requestBody = """
            {
                "name": "Penny",
                "password": "Penny#123"
            }
            """;

        mockMvc.perform(
                post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Email is required"));

        verifyNoInteractions(authService);
    }

    @Test
    void register_shouldReturn400ForInvalidEmail() throws Exception {

        NewUserRequest request = new NewUserRequest("Penny", "PennyTest.com", "Penny#123");

        mockMvc.perform(
                post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Email must be valid"));

        verifyNoInteractions(authService);
    }

    @Test
    void register_shouldReturn400ForInvalidPassword() throws Exception {

        NewUserRequest request = new NewUserRequest("Penny", "penny@test.com", "short");

        mockMvc.perform(
                post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Password must be between 8 and 100 characters"));

        verifyNoInteractions(authService);
    }

    @Test
    void register_shouldReturn409ForDuplicateEmail() throws Exception {

        NewUserRequest request = new NewUserRequest("Penny", "penny@test.com", "Penny#123");

        when(authService.register(any(NewUserRequest.class)))
                .thenThrow(new UserAlreadyExistsException());

        mockMvc.perform(
                post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("USER_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.message").value("User already exists with this email."));
    }
}