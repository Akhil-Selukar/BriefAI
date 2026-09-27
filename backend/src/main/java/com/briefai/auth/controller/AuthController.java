package com.briefai.auth.controller;

import com.briefai.auth.dto.*;
import com.briefai.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public NewUserResponse register(@Valid @RequestBody NewUserRequest request) {
        return authService.register(request);
    }

    @PostMapping("/verify-email")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void verifyEmail(@Valid @RequestBody VerifyEmailRequest request){
        authService.verifyEmail(request);
    }

    @PostMapping("/resend-otp")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resendOtp(@Valid @RequestBody ResendOtpRequest request) {
        authService.resendOtp(request);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(authService.login(request));
    }
}
