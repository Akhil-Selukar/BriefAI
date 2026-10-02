package com.briefai.conversation.controller;

import com.briefai.auth.dto.AuthenticatedUser;
import com.briefai.config.SecurityConfig;
import com.briefai.conversation.dto.ChatMessageResponse;
import com.briefai.conversation.dto.ConversationResponse;
import com.briefai.conversation.entity.ChatMessageRole;
import com.briefai.conversation.service.ConversationService;
import com.briefai.exception.conversation.ConversationNotFoundException;
import com.briefai.security.service.JwtService;
import com.briefai.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;

import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;

import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ConversationController.class)
@Import({SecurityConfig.class, ConversationControllerTest.TestSecurityConfiguration.class})
class ConversationControllerTest {

    @TestConfiguration
    @EnableWebSecurity
    static class TestSecurityConfiguration {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ConversationService conversationService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    private Authentication authentication;

    @BeforeEach
    void setUp() {
        authentication = new UsernamePasswordAuthenticationToken(new AuthenticatedUser(1L, "penny@test.com"), null, null);
    }

    private ConversationResponse conversationResponse() {
        return new ConversationResponse(100L, "Spring AI discussion",
                LocalDateTime.of(2026, 10, 1, 10, 0),
                LocalDateTime.of(2026, 10, 1, 10, 0));
    }

    @Test
    void create_shouldReturn201ForAuthenticatedUser() throws Exception {
        when(conversationService.create(1L, "Spring AI discussion")).thenReturn(conversationResponse());
        mockMvc.perform(post("/api/v1/conversations")
                        .with(authentication(authentication))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                   {
                                       "title": "Spring AI discussion"
                                   }
                                """)
                )
                .andExpect(status().isCreated());

        verify(conversationService).create(1L, "Spring AI discussion");
        verifyNoMoreInteractions(conversationService);
    }

    @Test
    void create_shouldReturn400ForBlankTitle() throws Exception {
        mockMvc.perform(post("/api/v1/conversations")
                .with(authentication(authentication))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                            {
                                "title": "   "
                            }
                        """)
        ).andExpect(status().isBadRequest());

        verifyNoInteractions(conversationService);
    }

    @Test
    void list_shouldReturnAuthenticatedUsersConversations() throws Exception {
        when(conversationService.list(1L)).thenReturn(List.of(conversationResponse()));

        mockMvc.perform(get("/api/v1/conversations")
                        .with(authentication(authentication))
                )
                .andExpect(status().isOk());

        verify(conversationService).list(1L);
        verifyNoMoreInteractions(conversationService);
    }

    @Test
    void get_shouldReturnConversationForAuthenticatedUser() throws Exception {
        when(conversationService.get(1L, 100L)).thenReturn(conversationResponse());

        mockMvc.perform(get("/api/v1/conversations/100")
                        .with(authentication(authentication))
                )
                .andExpect(status().isOk());

        verify(conversationService).get(1L, 100L);
        verifyNoMoreInteractions(conversationService);
    }

    @Test
    void messages_shouldReturnConversationHistory() throws Exception {
        ChatMessageResponse message = new ChatMessageResponse(500L, ChatMessageRole.USER, "What is RAG?",
                LocalDateTime.of(2026, 10, 1, 10, 5));

        when(conversationService.getMessages(1L, 100L)).thenReturn(List.of(message));

        mockMvc.perform(get("/api/v1/conversations/100/messages")
                        .with(authentication(authentication))
                )
                .andExpect(status().isOk());

        verify(conversationService).getMessages(1L, 100L);
        verifyNoMoreInteractions(conversationService);
    }

    @Test
    void delete_shouldReturn204() throws Exception {

        mockMvc.perform(delete("/api/v1/conversations/100")
                        .with(authentication(authentication))
                )
                .andExpect(status().isNoContent());

        verify(conversationService).delete(1L, 100L);
        verifyNoMoreInteractions(conversationService);
    }

    @Test
    void get_shouldReturn404ForForeignConversation() throws Exception {

        when(conversationService.get(1L, 100L)).thenThrow(new ConversationNotFoundException(""));

        mockMvc.perform(get("/api/v1/conversations/100")
                        .with(authentication(authentication))
                )
                .andExpect(status().isNotFound());

        verify(conversationService).get(1L, 100L);
        verifyNoMoreInteractions(conversationService);
    }
}