package com.briefai.rag.controller;

import com.briefai.auth.dto.AuthenticatedUser;
import com.briefai.config.SecurityConfig;
import com.briefai.rag.dto.RagResponse;
import com.briefai.rag.dto.RagSource;
import com.briefai.rag.service.RagService;
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

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChatController.class)
@Import({SecurityConfig.class, ChatControllerTest.TestSecurityConfiguration.class})
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RagService ragService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @TestConfiguration
    @EnableWebSecurity
    static class TestSecurityConfiguration {
    }

    @Test
    void ask_shouldPassAuthenticatedUserIdToRagService() throws Exception {

        Long userId = 1L;
        String question = "What is rag?";

        AuthenticatedUser principal = new AuthenticatedUser(userId, "penny@test.com");

        var authentication = new UsernamePasswordAuthenticationToken(principal, null, null);

        RagResponse response = new RagResponse("RAG combines retrieval and generation.",
                List.of(new RagSource(1, 100L, "test.pdf", 3, 0, 0.95)));

        when(ragService.answer(userId, question)).thenReturn(response);

        mockMvc.perform(post("/api/v1/chat/ask")
                        .with(authentication(authentication))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "question": "What is rag?"
                                }
                                """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("RAG combines retrieval and generation."))
                .andExpect(jsonPath("$.sources[0].documentId").value(100))
                .andExpect(jsonPath("$.sources[0].documentName").value("test.pdf"))
                .andExpect(jsonPath("$.sources[0].pageNumber").value(3));

        verify(ragService).answer(1L, question);
        verifyNoMoreInteractions(ragService);
    }
}