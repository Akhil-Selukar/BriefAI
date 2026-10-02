package com.briefai.conversation.service;

import com.briefai.conversation.dto.ChatMessageResponse;
import com.briefai.conversation.dto.ConversationResponse;
import com.briefai.conversation.entity.ChatMessage;
import com.briefai.conversation.entity.ChatMessageRole;
import com.briefai.conversation.entity.Conversation;
import com.briefai.conversation.repository.ChatMessageRepository;
import com.briefai.conversation.repository.ConversationRepository;
import com.briefai.exception.conversation.ConversationNotFoundException;
import com.briefai.exception.user.UserNotFoundException;
import com.briefai.user.entity.User;
import com.briefai.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final ChatMessageRepository messageRepository;
    private final UserRepository userRepository;

    public ConversationService(ConversationRepository conversationRepository, ChatMessageRepository messageRepository, UserRepository userRepository) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ConversationResponse create(Long userId, String title) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException("User not found."));
        Conversation conversation = conversationRepository.save(new Conversation(user, title));
        return toResponse(conversation);
    }

    @Transactional(readOnly = true)
    public List<ConversationResponse> list(Long userId) {
        return conversationRepository.findAllByUserIdOrderByUpdatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ConversationResponse get(Long userId, Long conversationId) {
        return toResponse(findOwnedConversation(userId, conversationId));
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getMessages(Long userId, Long conversationId) {
        findOwnedConversation(userId, conversationId);

        return messageRepository.findAllByConversationIdOrderByIdAsc(conversationId)
                .stream()
                .map(this::toMessageResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ChatMessage> getRecentMessages(Long userId, Long conversationId) {
        findOwnedConversation(userId, conversationId);

        List<ChatMessage> messages = new ArrayList<>(messageRepository.findTop10ByConversationIdOrderByIdDesc(conversationId));

        Collections.reverse(messages);
        return messages;
    }

    @Transactional
    public ChatMessageResponse addMessage(Long userId, Long conversationId, ChatMessageRole role, String content) {
        Conversation conversation = findOwnedConversation(userId, conversationId);
        ChatMessage message = messageRepository.save(new ChatMessage(conversation, role, content));
        conversation.touch();
        return toMessageResponse(message);
    }

    @Transactional
    public void delete(Long userId, Long conversationId) {
        Conversation conversation = findOwnedConversation(userId, conversationId);
        conversationRepository.delete(conversation);
    }

    private Conversation findOwnedConversation(Long userId, Long conversationId) {
        return conversationRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new ConversationNotFoundException("Conversation not found"));
    }

    private ConversationResponse toResponse(Conversation conversation) {
        return new ConversationResponse(conversation.getId(), conversation.getTitle(),
                conversation.getCreatedAt(), conversation.getUpdatedAt());
    }

    private ChatMessageResponse toMessageResponse(ChatMessage message) {
        return new ChatMessageResponse(message.getId(), message.getRole(), message.getContent(), message.getCreatedAt());
    }

    @Transactional
    public void addExchange(Long userId, Long conversationId, String question, String answer) {
        Conversation conversation = findOwnedConversation(userId, conversationId);
        ChatMessage userMessage = new ChatMessage(conversation, ChatMessageRole.USER, question);
        ChatMessage assistantMessage = new ChatMessage(conversation, ChatMessageRole.ASSISTANT, answer);

        messageRepository.saveAll(List.of(userMessage, assistantMessage));
        conversation.touch();
    }
}

