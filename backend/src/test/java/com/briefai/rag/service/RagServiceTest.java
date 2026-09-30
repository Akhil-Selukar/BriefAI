package com.briefai.rag.service;

import com.briefai.document.retrieval.dto.RetrievedChunk;
import com.briefai.document.retrieval.service.DocumentRetrievalService;
import com.briefai.exception.rag.ChatModelResponseException;
import com.briefai.rag.dto.RagResponse;
import com.briefai.rag.dto.RagSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RagServiceTest {
    @Mock
    private DocumentRetrievalService retrievalService;

    @Mock
    private ChatModel chatModel;

    @InjectMocks
    private RagService ragService;


    @Test
    void answer_shouldNotCallRetrievalServiceIfQuestionIsEmpty() {
        assertThrows(IllegalArgumentException.class, () -> ragService.answer(1L, "   "));
        verifyNoInteractions(retrievalService);
    }

    @Test
    void answer_shouldNotCallChatModelIfRetrievalServiceReturnNoChunks() {
        List<RetrievedChunk> chunks = List.of();
        when(retrievalService.search(any(), anyString())).thenReturn(chunks);

        RagResponse response = ragService.answer(1L, "what is rag?");

        assertEquals("I couldn't find relevant information in your uploaded documents.", response.getAnswer());
        assertEquals(0, response.getSources().size());
    }

    @Test
    void answer_shouldReturnAnswerWithSourceMetadata() {

        Long userId = 1L;
        String question = "What is rag?";
        RetrievedChunk chunk = new RetrievedChunk(100L, "test.pdf", 0, 3, "RAG combines document retrieval with text generation.", 0.95);

        when(retrievalService.search(userId, question)).thenReturn(List.of(chunk));

        ChatResponse chatResponse = mock(ChatResponse.class);
        Generation generation = mock(Generation.class);
        AssistantMessage assistantMessage = new AssistantMessage("RAG combines retrieval and generation.");

        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);
        when(chatResponse.getResult()).thenReturn(generation);
        when(generation.getOutput()).thenReturn(assistantMessage);

        RagResponse result = ragService.answer(userId, question);

        assertEquals("RAG combines retrieval and generation.", result.getAnswer());
        assertEquals(1, result.getSources().size());
        RagSource source = result.getSources().get(0);
        assertEquals(1, source.getSourceNumber());
        assertEquals(100L, source.getDocumentId());
        assertEquals("test.pdf", source.getDocumentName());
        assertEquals(3, source.getPageNumber());
        assertEquals(0, source.getChunkIndex());
        assertEquals(0.95, source.getSimilarity(), 0.0001);

        ArgumentCaptor<Prompt> promptCaptor = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).call(promptCaptor.capture());

        Prompt capturedPrompt = promptCaptor.getValue();

        assertEquals(2, capturedPrompt.getInstructions().size());
        assertInstanceOf(SystemMessage.class, capturedPrompt.getInstructions().get(0));
        assertInstanceOf(UserMessage.class, capturedPrompt.getInstructions().get(1));

        String userPrompt = capturedPrompt.getInstructions().get(1).getText();

        assertTrue(userPrompt.contains(question));
        assertTrue(userPrompt.contains("test.pdf"));
        assertTrue(userPrompt.contains("Page: 3"));
        assertTrue(userPrompt.contains(chunk.getContent()));

        InOrder order = inOrder(retrievalService, chatModel);

        order.verify(retrievalService).search(userId, question);
        order.verify(chatModel).call(any(Prompt.class));
    }

    @Test
    void answer_shouldPropagateChatModelFailure() {

        Long userId = 1L;
        String question = "What is rag?";
        RetrievedChunk chunk = new RetrievedChunk(100L, "test.pdf", 0, 1, "RAG combines retrieval with generation.", 0.92);

        when(retrievalService.search(userId, question)).thenReturn(List.of(chunk));

        RuntimeException failure = new RuntimeException("Ollama unavailable");
        when(chatModel.call(any(Prompt.class))).thenThrow(failure);

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> ragService.answer(userId, question));
        assertSame(failure, thrown);
        verify(retrievalService).search(userId, question);
        verify(chatModel).call(any(Prompt.class));
    }

    @Test
    void answer_shouldRejectEmptyChatModelResponse() {

        Long userId = 1L;
        String question = "What is rag?";
        RetrievedChunk chunk = new RetrievedChunk(100L, "test.pdf", 0, 1, "RAG combines retrieval with generation.", 0.92);

        when(retrievalService.search(userId, question)).thenReturn(List.of(chunk));

        ChatResponse chatResponse = mock(ChatResponse.class);
        Generation generation = mock(Generation.class);

        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);
        when(chatResponse.getResult()).thenReturn(generation);
        when(generation.getOutput()).thenReturn(new AssistantMessage("   "));

        ChatModelResponseException thrown = assertThrows(ChatModelResponseException.class, () -> ragService.answer(userId, question));

        assertEquals("The chat model returned an empty answer.", thrown.getMessage());
    }
}