package com.briefai.email;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class SmtpEmailService implements EmailService {

    private JavaMailSender mailSender;
    private String fromAddress;

    public SmtpEmailService(JavaMailSender mailSender, @Value("${app.email.from}") String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    @Override
    public void sendVerificationOtp(String toEmail, String toName, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(fromAddress);
        message.setTo(toEmail);
        message.setSubject("Verify your BriefAI email");
        message.setText("""
                Hi %s,

                Your BriefAI verification code is:

                <b>%s</b>

                This code expires in 5 minutes.

                If you did not create a BriefAI account, please ignore this email.

                Thanks,
                BriefAI
                """.formatted(toName, otp));

        mailSender.send(message);
    }
}
