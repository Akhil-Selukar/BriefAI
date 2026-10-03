package com.briefai.conversation.service;

import com.briefai.conversation.dto.ChatMessageResponse;
import com.briefai.conversation.dto.ChatMessageSourceResponse;
import com.briefai.conversation.dto.ConversationResponse;
import com.briefai.conversation.entity.ChatMessage;
import com.briefai.conversation.entity.ChatMessageRole;
import com.briefai.conversation.entity.ChatMessageSource;
import com.briefai.conversation.entity.Conversation;
import com.briefai.conversation.repository.ChatMessageRepository;
import com.briefai.conversation.repository.ChatMessageSourceRepository;
import com.briefai.conversation.repository.ConversationRepository;
import com.briefai.document.entity.Document;
import com.briefai.document.repository.DocumentRepository;
import com.briefai.exception.conversation.ConversationNotFoundException;
import com.briefai.exception.user.UserNotFoundException;
import com.briefai.rag.dto.RagSource;
import com.briefai.user.entity.User;
import com.briefai.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class ConversationService {

    private static final Logger logger = LoggerFactory.getLogger(ConversationService.class);
    private final ConversationRepository conversationRepository;
    private final ChatMessageRepository messageRepository;
    private final UserRepository userRepository;

    private final ChatMessageSourceRepository sourceRepository;
    private final DocumentRepository documentRepository;

    public ConversationService(ConversationRepository conversationRepository, ChatMessageRepository messageRepository, UserRepository userRepository
            , ChatMessageSourceRepository sourceRepository, DocumentRepository documentRepository) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.sourceRepository = sourceRepository;
        this.documentRepository = documentRepository;
    }

    @Transactional
    public ConversationResponse create(Long userId, String title) {
        logger.debug("Creating new conversation with title {}", title);
        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException("User not found."));
        Conversation conversation = conversationRepository.save(new Conversation(user, title));
        logger.debug("New conversation created");
        return toResponse(conversation);
    }

    @Transactional(readOnly = true)
    public List<ConversationResponse> list(Long userId) {
        logger.debug("Fetching all conversations");
        return conversationRepository.findAllByUserIdOrderByUpdatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ConversationResponse get(Long userId, Long conversationId) {
        logger.debug("Fetching conversation with id {}", conversationId);
        return toResponse(findOwnedConversation(userId, conversationId));
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getMessages(Long userId, Long conversationId) {
        logger.debug("Fetching all messages from conversation {}", conversationId);
        findOwnedConversation(userId, conversationId);   // this will throw exception in case the conversation is not owned by this user.

        return messageRepository.findAllByConversationIdOrderByIdAsc(conversationId)
                .stream()
                .map(this::toMessageResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ChatMessage> getRecentMessages(Long userId, Long conversationId) {
        logger.debug("Fetching the recent 10 messages from the conversation");
        findOwnedConversation(userId, conversationId);   // this will throw exception in case the conversation is not owned by this user.

        List<ChatMessage> messages = new ArrayList<>(messageRepository.findTop10ByConversationIdOrderByIdDesc(conversationId));

        Collections.reverse(messages);
        return messages;
    }

    @Transactional
    public ChatMessageResponse addMessage(Long userId, Long conversationId, ChatMessageRole role, String content) {
        logger.debug("Adding message to the conversation with id {}", conversationId);
        Conversation conversation = findOwnedConversation(userId, conversationId);
        ChatMessage message = messageRepository.save(new ChatMessage(conversation, role, content));
        conversation.touch();
        return toMessageResponse(message);
    }

    @Transactional
    public void delete(Long userId, Long conversationId) {
        logger.debug("Deleting the conversation with id {}", conversationId);
        Conversation conversation = findOwnedConversation(userId, conversationId);
        conversationRepository.delete(conversation);
    }

    private Conversation findOwnedConversation(Long userId, Long conversationId) {
        logger.debug("Checking if the conversation is owned by the user or not");
        return conversationRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new ConversationNotFoundException("Conversation not found"));
    }

    private ConversationResponse toResponse(Conversation conversation) {
        return new ConversationResponse(conversation.getId(), conversation.getTitle(),
                conversation.getCreatedAt(), conversation.getUpdatedAt());
    }

    private ChatMessageResponse toMessageResponse(ChatMessage message) {
        List<ChatMessageSourceResponse> sources = sourceRepository.findAllByMessageIdOrderBySourceNumberAsc(message.getId())
                        .stream()
                        .map(this::toSourceResponse)
                        .toList();

        return new ChatMessageResponse(message.getId(), message.getRole(), message.getContent(), message.getCreatedAt(), sources);
    }

    private ChatMessageSourceResponse toSourceResponse(ChatMessageSource source) {
        Long documentId = source.getDocument() != null ? source.getDocument().getId() : null;

        return new ChatMessageSourceResponse(source.getSourceNumber(), documentId, source.getDocumentName(), source.getPageNumber(),
                source.getChunkIndex(), source.getSimilarity());
    }

    @Transactional
    public void addExchange(Long userId, Long conversationId, String question, String answer, List<RagSource> sources) {
        logger.debug("Adding the conversation exchange between user and agent");
        Conversation conversation = findOwnedConversation(userId, conversationId);
        messageRepository.save(new ChatMessage(conversation, ChatMessageRole.USER, question));

        ChatMessage assistantMessage = messageRepository.save(new ChatMessage(conversation, ChatMessageRole.ASSISTANT, answer));
        if (sources != null && !sources.isEmpty()) {
            List<ChatMessageSource> persistedSources = new ArrayList<>();

            for (RagSource source : sources) {
                Document document = documentRepository.findByIdAndUserId(source.getDocumentId(), userId).orElse(null);

                persistedSources.add(new ChatMessageSource(
                        assistantMessage,
                        document,
                        source.getSourceNumber(),
                        source.getDocumentName(),
                        source.getPageNumber(),
                        source.getChunkIndex(),
                        source.getSimilarity()
                        )
                );
            }
            sourceRepository.saveAll(persistedSources);
        }
        conversation.touch();
    }
}

