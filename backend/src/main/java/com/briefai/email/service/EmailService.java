package com.briefai.email.service;

public interface EmailService {
    void sendVerificationOtp(String toEmail, String toName, String otp);
}
