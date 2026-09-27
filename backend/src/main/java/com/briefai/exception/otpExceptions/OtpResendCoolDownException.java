package com.briefai.exception.otpExceptions;

public class OtpResendCoolDownException extends RuntimeException {
    public OtpResendCoolDownException(String message) {
        super(message);
    }
}
