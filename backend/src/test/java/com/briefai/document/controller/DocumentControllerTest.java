package com.briefai.document.controller;

import com.briefai.auth.dto.AuthenticatedUser;
import com.briefai.config.SecurityConfig;
import com.briefai.document.dto.DocumentResponse;
import com.briefai.document.entity.DocumentStatus;
import com.briefai.document.service.DocumentService;
import com.briefai.security.service.JwtService;
import com.briefai.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DocumentController.class)
@Import({SecurityConfig.class, DocumentControllerTest.TestSecurityConfiguration.class})
class DocumentControllerTest {
    @TestConfiguration
    @EnableWebSecurity
    static class TestSecurityConfiguration {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DocumentService documentService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    void uploadDocument_shouldReturn401WithoutAuthentication() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", "test document".getBytes());
        mockMvc.perform(multipart("/api/v1/documents").file(file)).andExpect(status().isForbidden());
        verifyNoInteractions(documentService);
    }

    @Test
    void uploadDocument_shouldReturn201ForAuthenticatedUser() throws Exception {

        AuthenticatedUser principal = new AuthenticatedUser(1L, "penny@test.com");
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(principal, null, null);
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", "test document".getBytes());

        DocumentResponse response = new DocumentResponse(100L, "test.pdf", "application/pdf",
                file.getSize(), null, DocumentStatus.UPLOADED, LocalDateTime.of(2026, 9, 28, 22, 30));

        when(documentService.uploadDocument(eq(42L), any(MultipartFile.class))).thenReturn(response);

        mockMvc.perform(multipart("/api/v1/documents").file(file).with(authentication(auth))).andExpect(status().isCreated());
        verify(documentService).uploadDocument(eq(1L), any(MultipartFile.class));
    }
}