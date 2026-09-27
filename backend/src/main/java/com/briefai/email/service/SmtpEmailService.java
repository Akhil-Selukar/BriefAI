package com.briefai.email.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class SmtpEmailService implements EmailService {
    private static final Logger logger = LoggerFactory.getLogger(SmtpEmailService.class);
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
        logger.debug("Sending OTP email for user");
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject("Verify your BriefAI email");
            String htmlContent = emailTemplateService.renderVerificationOtp(toName, otp, expirationMinutes);

            helper.setText(htmlContent, true);

            mailSender.send(message);
            logger.debug("Email sent successfully.");
        } catch (MessagingException e) {
            logger.error("Email could not be sent.");
            throw new RuntimeException("Failed to send HTML email", e);
        }
    }
}
