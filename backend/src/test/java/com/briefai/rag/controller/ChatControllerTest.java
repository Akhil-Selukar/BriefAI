package com.briefai.rag.controller;

import com.briefai.auth.dto.AuthenticatedUser;
import com.briefai.config.SecurityConfig;
import com.briefai.rag.dto.RagResponse;
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
    void ask_shouldUseConversationAwareRagWhenConversationIdProvided() throws Exception {
        RagResponse response = new RagResponse("Document grounding explanation [1]", List.of());
        when(ragService.answer(1L, 100L, "Explain the second point.")).thenReturn(response);

        mockMvc.perform(post("/api/v1/chat/ask")
                .with(authentication(getAuthenticatedUser()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                       {
                            "conversationId": 100,
                            "question": "Explain the second point."
                       }
                """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("Document grounding explanation [1]"));

        verify(ragService).answer(1L, 100L, "Explain the second point.");
        verify(ragService, never()).answer(anyLong(), anyString());
    }

    @Test
    void ask_shouldUseStatelessRagWhenConversationIdMissing() throws Exception {
        RagResponse response = new RagResponse("RAG combines retrieval and generation. [1]", List.of());
        when(ragService.answer(1L, "What is RAG?")).thenReturn(response);

        mockMvc.perform(post("/api/v1/chat/ask")
                .with(authentication(getAuthenticatedUser()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "question": "What is RAG?"
                        }
                """)
                )
                .andExpect(status().isOk());

        verify(ragService).answer(1L, "What is RAG?");
        verify(ragService, never()).answer(anyLong(), anyLong(), anyString());
    }

    private UsernamePasswordAuthenticationToken getAuthenticatedUser() {
        AuthenticatedUser principal = new AuthenticatedUser(1L, "penny@test.com");
        return new UsernamePasswordAuthenticationToken(principal, null, null);
    }
}