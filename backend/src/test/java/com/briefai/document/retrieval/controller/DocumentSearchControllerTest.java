package com.briefai.document.retrieval.controller;

import com.briefai.auth.dto.AuthenticatedUser;
import com.briefai.config.SecurityConfig;
import com.briefai.document.retrieval.dto.DocumentSearchRequest;
import com.briefai.document.retrieval.dto.RetrievedChunk;
import com.briefai.document.retrieval.service.DocumentRetrievalService;
import com.briefai.security.service.JwtService;
import com.briefai.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DocumentSearchController.class)
@Import({SecurityConfig.class, DocumentSearchControllerTest.TestSecurityConfiguration.class})
class DocumentSearchControllerTest {

    @TestConfiguration
    @EnableWebSecurity
    static class TestSecurityConfiguration {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private DocumentRetrievalService retrievalService;

    @Autowired
    private ObjectMapper objectMapper;


    @Test
    void search_shouldReturn401WithoutAuthentication() throws Exception {
        DocumentSearchRequest request = new DocumentSearchRequest("What is RAG?");
        mockMvc.perform(post("/api/v1/documents/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        ).andExpect(status().isForbidden());

        verifyNoInteractions(retrievalService);
    }

    @Test
    void search_shouldReturnChunksForAuthenticatedUser() throws Exception {
        RetrievedChunk chunk = new RetrievedChunk(100L, "research.pdf", 0, 1,
                "RAG is a technique of generate better AI model response by searching outside the models knowledge base.", 0.95);
        List<RetrievedChunk> result = List.of(chunk);

        DocumentSearchRequest request = new DocumentSearchRequest("What is RAG?");

        when(retrievalService.search(1L, request.getQuestion())).thenReturn(result);

        mockMvc.perform(post("/api/v1/documents/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(authentication(getAuthenticatedUser()))
        ).andExpect(status().isOk());
    }

    @Test
    void search_shouldReturn400ForBlankQuestion() throws Exception {
        DocumentSearchRequest request = new DocumentSearchRequest("   ");
        doThrow(new IllegalArgumentException()).when(retrievalService).search(1L, request.getQuestion());

        mockMvc.perform(post("/api/v1/documents/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(authentication(getAuthenticatedUser()))
        ).andExpect(status().isBadRequest());
    }

    @Test
    void search_shouldReturn400ForQuestionOver2000Characters() throws Exception {
        DocumentSearchRequest request = new DocumentSearchRequest("Question".repeat(251));

        mockMvc.perform(post("/api/v1/documents/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(authentication(getAuthenticatedUser()))
        ).andExpect(status().isBadRequest());

        verifyNoInteractions(retrievalService);
    }

    private UsernamePasswordAuthenticationToken getAuthenticatedUser() {
        AuthenticatedUser principal = new AuthenticatedUser(1L, "penny@test.com");
        return new UsernamePasswordAuthenticationToken(principal, null, null);
    }

}