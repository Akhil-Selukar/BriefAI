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
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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

        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", "test document".getBytes());

        DocumentResponse response = new DocumentResponse(100L, "test.pdf", "application/pdf",
                file.getSize(), null, DocumentStatus.UPLOADED, LocalDateTime.of(2026, 9, 28, 22, 30));

        when(documentService.uploadDocument(eq(42L), any(MultipartFile.class))).thenReturn(response);

        mockMvc.perform(multipart("/api/v1/documents").file(file).with(authentication(getAuthenticatedUser()))).andExpect(status().isCreated());
        verify(documentService).uploadDocument(eq(1L), any(MultipartFile.class));
    }

    @Test
    void getDocuments_shouldReturn200ForAuthenticatedUser() throws Exception {

        DocumentResponse document1 = new DocumentResponse(1L, "test1.pdf", "application/pdf", 100L,
                null, DocumentStatus.UPLOADED, LocalDateTime.of(2026, 9, 28, 23, 0));
        DocumentResponse document2 = new DocumentResponse(2L, "test2.pdf", "application/pdf", 200L,
                10, DocumentStatus.READY, LocalDateTime.of(2026, 9, 28, 22, 0));

        when(documentService.getDocuments(1L)).thenReturn(List.of(document1, document2));

        mockMvc.perform(get("/api/v1/documents")
                        .with(authentication(getAuthenticatedUser()))
                ).andExpect(status().isOk());

        verify(documentService).getDocuments(1L);
    }

    @Test
    void getDocument_shouldReturn200ForAuthenticatedUser() throws Exception {

        DocumentResponse response = new DocumentResponse(100L, "test1.pdf", "application/pdf", 100L,
                        null, DocumentStatus.UPLOADED, LocalDateTime.of(2026, 9, 28, 23, 0));

        when(documentService.getDocument(1L, 100L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/documents/100")
                        .with(authentication(getAuthenticatedUser()))
                )
                .andExpect(status().isOk());

        verify(documentService).getDocument(1L, 100L);
    }

    @Test
    void deleteDocument_shouldReturn204ForAuthenticatedUser() throws Exception {

        mockMvc.perform(delete("/api/v1/documents/100")
                        .with(authentication(getAuthenticatedUser()))
                )
                .andExpect(status().isNoContent());

        verify(documentService).deleteDocument(1L, 100L);
    }

    @Test
    void getDocuments_shouldReturn401WithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/documents"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(documentService);
    }

    private UsernamePasswordAuthenticationToken getAuthenticatedUser() {
        AuthenticatedUser principal = new AuthenticatedUser(1L, "penny@test.com");
        return new UsernamePasswordAuthenticationToken(principal, null, null);
    }
}