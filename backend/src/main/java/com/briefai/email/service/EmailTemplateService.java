package com.briefai.email.service;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.util.HtmlUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

@Service
public class EmailTemplateService {

    public String renderVerificationOtp(String name, String otp, long expirationMinutes) {
        String template = loadTemplate("templates/email/verification-otp.html");

        return template.replace("{{name}}", HtmlUtils.htmlEscape(name))
                .replace("{{otp}}", HtmlUtils.htmlEscape(otp))
                .replace("{{expirationMinutes}}", String.valueOf(expirationMinutes));
    }

    private String loadTemplate(String path) {
        ClassPathResource resource = new ClassPathResource(path);

        try {
            InputStreamReader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8);
            return FileCopyUtils.copyToString(reader);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load email template: " + path, e);
        }
    }
}
