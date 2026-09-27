package com.briefai.auth.controller;

import com.briefai.auth.dto.NewUserRequest;
import com.briefai.auth.dto.NewUserResponse;
import com.briefai.auth.dto.ResendOtpRequest;
import com.briefai.auth.dto.VerifyEmailRequest;
import com.briefai.auth.service.AuthService;
import com.briefai.exception.otpExceptions.InvalidOtpException;
import com.briefai.exception.otpExceptions.OtpAttemptsExceededException;
import com.briefai.exception.otpExceptions.OtpExpiredException;
import com.briefai.exception.otpExceptions.OtpResendCoolDownException;
import com.briefai.exception.user.UserAlreadyExistsException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
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

    @Test
    void verifyEmail_shouldReturn204ForValidOtp() throws Exception {
        String requestBody = """
            {
                "email": "penny@test.com",
                "otp": "123456"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                )
                .andExpect(status().isNoContent());

        verify(authService).verifyEmail(any(VerifyEmailRequest.class));
    }

    @Test
    void verifyEmail_shouldReturn400ForInvalidOtpFormat() throws Exception {
        String requestBody = """
            {
                "email": "penny@test.com",
                "otp": "123"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("OTP must be of 6 digits"));

        verifyNoInteractions(authService);
    }

    @Test
    void verifyEmail_shouldReturn400ForWrongOtp() throws Exception {

        doThrow(new InvalidOtpException("Invalid OTP.")).when(authService).verifyEmail(any(VerifyEmailRequest.class));

        String requestBody = """
            {
                "email": "penny@test.com",
                "otp": "999999"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_OTP"))
                .andExpect(jsonPath("$.message").value("Invalid OTP."));
    }

    @Test
    void verifyEmail_shouldReturn400ForExpiredOtp() throws Exception {

        doThrow(new OtpExpiredException("OTP has expired.")).when(authService).verifyEmail(any(VerifyEmailRequest.class));

        String requestBody = """
            {
                "email": "penny@test.com",
                "otp": "123456"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("OTP_EXPIRED"))
                .andExpect(jsonPath("$.message").value("OTP has expired."));
    }

    @Test
    void verifyEmail_shouldReturn429WhenAttemptsExceeded() throws Exception {

        doThrow(new OtpAttemptsExceededException("Maximum OTP verification attempts exceeded.")).when(authService).verifyEmail(any(VerifyEmailRequest.class));

        String requestBody = """
            {
                "email": "penny@test.com",
                "otp": "999999"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                )
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").value("OTP_ATTEMPT_EXHAUSTED"))
                .andExpect(jsonPath("$.message").value("Maximum OTP verification attempts exceeded."));
    }

    @Test
    void resendOtp_shouldReturn204ForSuccessfulResend() throws Exception {

        mockMvc.perform(post("/api/v1/auth/resend-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "email": "penny@test.com"
                            }
                            """)
                )
                .andExpect(status().isNoContent());

        verify(authService).resendOtp(any(ResendOtpRequest.class));
    }

    @Test
    void resendOtp_shouldReturn400ForInvalidEmail() throws Exception {

        mockMvc.perform(post("/api/v1/auth/resend-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "email": "pennyAtTest.com"
                            }
                            """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));

        verifyNoInteractions(authService);
    }

    @Test
    void resendOtp_shouldReturn429DuringCoolDownTime() throws Exception {
        doThrow(new OtpResendCoolDownException("Please wait before requesting another OTP.")).when(authService)
                .resendOtp(any(ResendOtpRequest.class));

        mockMvc.perform(post("/api/v1/auth/resend-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "email": "penny@test.com"
                            }
                            """)
                )
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").value("OTP_RESEND_COOLDOWN"));
    }


}