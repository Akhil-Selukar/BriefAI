package com.briefai.email.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class SmtpEmailService implements EmailService {

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final EmailTemplateService emailTemplateService;
    private final long expirationMinutes;

    public SmtpEmailService(JavaMailSender mailSender, @Value("${app.email.from}") String fromAddress,
                            EmailTemplateService emailTemplateService, @Value("${app.otp.expiration-minutes}") long expirationMinutes) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
        this.emailTemplateService = emailTemplateService;
        this.expirationMinutes = expirationMinutes;
    }

    @Override
    public void sendVerificationOtp(String toEmail, String toName, String otp) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject("Verify your BriefAI email");
            String htmlContent = emailTemplateService.renderVerificationOtp(toName, otp, expirationMinutes);

            helper.setText(htmlContent, true);

            mailSender.send(message);

        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send HTML email", e);
        }
    }
}
