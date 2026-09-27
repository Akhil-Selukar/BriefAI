package com.briefai.email;

public interface EmailService {
    void sendVerificationOtp(String toEmail, String toName, String otp);
}
