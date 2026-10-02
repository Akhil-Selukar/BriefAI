package com.briefai.conversation.service;

import com.briefai.conversation.entity.ChatMessage;
import com.briefai.conversation.entity.ChatMessageRole;
import com.briefai.conversation.entity.Conversation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConversationQueryServiceTest {

    @Mock
    private ChatModel chatModel;

    @InjectMocks
    private ConversationQueryService queryService;

    @Test
    void rewrite_shouldReturnOriginalQuestionWhenHistoryEmpty() {
        String result = queryService.rewriteQuestion(List.of(), "What is RAG?");

        assertEquals("What is RAG?", result);
        verifyNoInteractions(chatModel);
    }

    @Test
    void rewrite_shouldUseHistoryForFollowUpQuestion() {
        Conversation conversation = mock(Conversation.class);

        ChatMessage userMessage = new ChatMessage(conversation, ChatMessageRole.USER, "What are the advantages of RAG?");
        ChatMessage assistantMessage = new ChatMessage(conversation, ChatMessageRole.ASSISTANT, " 1. Current information. 2. Document grounding.");

        ChatResponse response = mock(ChatResponse.class);
        Generation generation = mock(Generation.class);

        when(chatModel.call(any(Prompt.class))).thenReturn(response);
        when(response.getResult()).thenReturn(generation);
        when(generation.getOutput()).thenReturn(new AssistantMessage("Explain document grounding in RAG."));

        String result = queryService.rewriteQuestion(List.of(userMessage, assistantMessage), "Explain the second point.");

        assertEquals("Explain document grounding in RAG.", result);
    }
}