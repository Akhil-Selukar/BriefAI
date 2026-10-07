package com.briefai.conversation.service;

import com.briefai.conversation.dto.ChatMessageResponse;
import com.briefai.conversation.dto.ConversationResponse;
import com.briefai.conversation.entity.ChatMessage;
import com.briefai.conversation.entity.ChatMessageRole;
import com.briefai.conversation.entity.ChatMessageSource;
import com.briefai.conversation.entity.Conversation;
import com.briefai.conversation.repository.ChatMessageRepository;
import com.briefai.conversation.repository.ChatMessageSourceRepository;
import com.briefai.conversation.repository.ConversationRepository;
import com.briefai.document.document.entity.Document;
import com.briefai.document.document.repository.DocumentRepository;
import com.briefai.exception.conversation.ConversationNotFoundException;
import com.briefai.exception.user.UserNotFoundException;
import com.briefai.rag.dto.RagSource;
import com.briefai.user.entity.User;
import com.briefai.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConversationServiceTest {

    @Mock
    private ConversationRepository conversationRepository;
    @Mock
    private ChatMessageRepository messageRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ChatMessageSourceRepository sourceRepository;
    @Mock
    private DocumentRepository documentRepository;

    private ConversationService conversationService;

    @BeforeEach
    void setUp() {
        conversationService = new ConversationService(conversationRepository, messageRepository, userRepository, sourceRepository, documentRepository);
    }

    @Test
    void create_shouldSaveConversationForUser(){
        User user = new User("Penny", "penny@test.com", "23asfadsaS#fgds");
        Conversation conversation = new Conversation(user, "conversation 1");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(conversationRepository.save(any(Conversation.class))).thenReturn(conversation);

        ConversationResponse savedConversation = conversationService.create(1L, "conversation 1");
        assertEquals(conversation.getTitle(), savedConversation.getTitle());
    }

    @Test
    void create_shouldThrowUserNotFoundForInvalidUser() {
        doThrow(new UserNotFoundException("User not found")).when(userRepository).findById(1L);

        assertThrows(UserNotFoundException.class, ()-> conversationService.create(1L, "conversation 1"));
        verifyNoInteractions(conversationRepository);
    }

    @Test
    void list_shouldReturnOnlyUsersConversations() {
        User user = new User("Penny", "penny@test.com", "23asfadsaS#fgds");
        List<Conversation> conversations = List.of(
                new Conversation(user, "conversation 1"),
                new Conversation(user, "conversation 2")
        );

        when(conversationRepository.findAllByUserIdOrderByUpdatedAtDesc(1L)).thenReturn(conversations);

        List<ConversationResponse> response = conversationService.list(1L);

        assertEquals(2, response.size());
        assertEquals("conversation 1", response.get(0).getTitle());
        assertEquals("conversation 2", response.get(1).getTitle());
    }

    @Test
    void getMessages_shouldRejectForeignConversation() {
        Long userId = 1L;
        Long conversationId = 100L;

        when(conversationRepository.findByIdAndUserId(conversationId, userId)).thenReturn(Optional.empty());

        assertThrows(ConversationNotFoundException.class, () -> conversationService.getMessages(userId, conversationId));
        verifyNoInteractions(messageRepository);
    }

    @Test
    void getRecentMessages_shouldReturnChronologicalOrder() {
        Long userId = 42L;
        Long conversationId = 100L;

        Conversation conversation = mock(Conversation.class);
        ChatMessage message1 = mock(ChatMessage.class);
        ChatMessage message2 = mock(ChatMessage.class);
        ChatMessage message3 = mock(ChatMessage.class);

        when(conversationRepository.findByIdAndUserId(conversationId, userId)).thenReturn(Optional.of(conversation));
        when(messageRepository.findTop10ByConversationIdOrderByIdDesc(conversationId)).thenReturn(List.of(message1, message2, message3));

        List<ChatMessage> result = conversationService.getRecentMessages(userId, conversationId);
        assertEquals(List.of(message3, message2, message1), result);
    }

    @Test
    void addMessage_shouldPersistMessageAndUpdateActivity() {

        Long userId = 42L;
        Long conversationId = 100L;
        User owner = mock(User.class);
        Conversation conversation = new Conversation(owner, "Spring AI discussion");

        ReflectionTestUtils.setField(conversation, "id", conversationId);
        LocalDateTime oldUpdatedAt = LocalDateTime.now().minusDays(1);
        ReflectionTestUtils.setField(conversation, "updatedAt", oldUpdatedAt);

        when(conversationRepository.findByIdAndUserId(conversationId, userId)).thenReturn(Optional.of(conversation));
        when(messageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> {
            ChatMessage message = invocation.getArgument(0);

            ReflectionTestUtils.setField(message, "id", 500L);
            ReflectionTestUtils.setField(message, "createdAt", LocalDateTime.now());

            return message;
        });

        ChatMessageResponse response = conversationService.addMessage(userId, conversationId, ChatMessageRole.USER, "What is RAG?");

        assertEquals(500L, response.getId());
        assertEquals(ChatMessageRole.USER, response.getRole());
        assertEquals("What is RAG?", response.getContent());
        assertNotNull(response.getCreatedAt());
        assertTrue(conversation.getUpdatedAt().isAfter(oldUpdatedAt));
    }

    @Test
    void delete_shouldRejectForeignConversation() {
        Long userId = 42L;
        Long conversationId = 100L;

        when(conversationRepository.findByIdAndUserId(conversationId, userId)).thenReturn(Optional.empty());

        assertThrows(ConversationNotFoundException.class, () -> conversationService.delete(userId, conversationId));
        verify(conversationRepository, never()).delete(any(Conversation.class));
        verifyNoInteractions(messageRepository);
    }

    @Test
    void addExchange_shouldPersistUserAndAssistantMessages() {
        Long userId = 1L;
        Long conversationId = 100L;
        Conversation conversation = mock(Conversation.class);
        Document document = mock(Document.class);

        when(conversationRepository.findByIdAndUserId(conversationId, userId)).thenReturn(Optional.of(conversation));
        when(documentRepository.findByIdAndUserId(10L, userId)).thenReturn(Optional.of(document));
        when(messageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RagSource source = new RagSource(1, 10L, "rag.pdf", 5, 2, 0.94);

        conversationService.addExchange(userId, conversationId, "What is RAG?", "RAG uses retrieved context. [1]", List.of(source));

        ArgumentCaptor<ChatMessage> messageCaptor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(messageRepository, times(2)).save(messageCaptor.capture());
        List<ChatMessage> messages = messageCaptor.getAllValues();
        assertEquals(ChatMessageRole.USER, messages.get(0).getRole());
        assertEquals(ChatMessageRole.ASSISTANT, messages.get(1).getRole());
        ArgumentCaptor<List<ChatMessageSource>> sourceCaptor = ArgumentCaptor.forClass(List.class);

        verify(sourceRepository).saveAll(sourceCaptor.capture());

        List<ChatMessageSource> persisted = sourceCaptor.getValue();

        assertEquals(1, persisted.size());

        ChatMessageSource savedSource = persisted.get(0);

        assertEquals(1, savedSource.getSourceNumber());
        assertEquals("rag.pdf", savedSource.getDocumentName());
        assertEquals(5, savedSource.getPageNumber());
        assertEquals(2, savedSource.getChunkIndex());
        assertEquals(0.94, savedSource.getSimilarity(), 0.000001);
        assertSame(document, savedSource.getDocument());
        assertSame(messages.get(1), savedSource.getMessage());
        verify(conversation).touch();
    }

    @Test
    void addExchange_shouldRejectForeignConversation() {
        when(conversationRepository.findByIdAndUserId(100L, 1L)).thenReturn(Optional.empty());

        assertThrows(ConversationNotFoundException.class, () -> conversationService.addExchange(1L, 100L, "Question", "Answer", List.of()));
        verifyNoInteractions(messageRepository);
    }
}