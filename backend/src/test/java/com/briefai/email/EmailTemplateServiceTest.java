package com.briefai.email;

import com.briefai.email.service.EmailTemplateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EmailTemplateServiceTest {
    private EmailTemplateService emailTemplateService;

    @BeforeEach
    void setUp() {
        emailTemplateService = new EmailTemplateService();
    }

    @Test
    void renderVerificationOtp_shouldInsertDynamicValues() {

        String html = emailTemplateService.renderVerificationOtp("Penny", "123456", 5);

        assertTrue(html.contains("Penny"));
        assertTrue(html.contains("123456"));
        assertTrue(html.contains("5"));
    }

    @Test
    void renderVerificationOtp_shouldEscapeUserProvidedName() {
        String html = emailTemplateService.renderVerificationOtp("<script>alert('otp')</script>", "123456", 5);

        assertFalse(html.contains("<script>"));
        assertTrue(html.contains("&lt;script&gt;"));
    }
}