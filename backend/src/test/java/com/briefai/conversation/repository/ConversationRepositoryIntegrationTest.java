package com.briefai.conversation.repository;

import com.briefai.conversation.entity.ChatMessage;
import com.briefai.conversation.entity.ChatMessageRole;
import com.briefai.conversation.entity.Conversation;
import com.briefai.user.entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@EnabledIfEnvironmentVariable(named = "RUN_DATABASE_INTEGRATION_TESTS", matches = "true")
class ConversationRepositoryIntegrationTest {

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private ChatMessageRepository messageRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long createUser() {
        return jdbcTemplate.queryForObject(
                """
                        INSERT INTO users (
                            name, email, password, email_verified,
                            created_at, updated_at
                        )
                        VALUES (
                            'Conversation Test',
                            ?,
                            'unused-test-password',
                            true,
                            CURRENT_TIMESTAMP,
                            CURRENT_TIMESTAMP
                        )
                        RETURNING id
                        """,
                Long.class, "testuser-" + UUID.randomUUID() + "@example.com"
        );
    }

    private User userReference(Long userId) {
        return entityManager.getReference(
                User.class,
                userId
        );
    }

    @Test
    void save_shouldPersistConversationWithOwnerAndTimestamps() {

        Long userId = createUser();

        Conversation conversation = new Conversation(userReference(userId), "Questions about Spring AI");
        Conversation saved = conversationRepository.saveAndFlush(conversation);

        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());

        entityManager.clear();

        Conversation persisted = conversationRepository.findById(saved.getId()).orElseThrow();

        assertEquals("Questions about Spring AI", persisted.getTitle());
        assertEquals(userId, persisted.getUser().getId());
        assertNotNull(persisted.getCreatedAt());
        assertNotNull(persisted.getUpdatedAt());
    }

    @Test
    void findAllByUser_shouldReturnOnlyOwnedConversationsNewestFirst() {

        Long userA = createUser();
        Long userB = createUser();

        Conversation older = conversationRepository.saveAndFlush(new Conversation(userReference(userA), "Older conversation"));
        Conversation newer = conversationRepository.saveAndFlush(new Conversation(userReference(userA), "Newer conversation"));
        Conversation foreign = conversationRepository.saveAndFlush(new Conversation(userReference(userB), "Another user's conversation"));

        jdbcTemplate.update(
                """
                        UPDATE conversations
                        SET updated_at = ?
                        WHERE id = ?
                        """,
                LocalDateTime.of(2026, 1, 1, 10, 0),
                older.getId()
        );

        jdbcTemplate.update(
                """
                        UPDATE conversations
                        SET updated_at = ?
                        WHERE id = ?
                        """,
                LocalDateTime.of(2026, 1, 2, 10, 0),
                newer.getId()
        );

        entityManager.clear();

        List<Conversation> results = conversationRepository.findAllByUserIdOrderByUpdatedAtDesc(userA);

        assertEquals(2, results.size());
        assertEquals(newer.getId(), results.get(0).getId());
        assertEquals(older.getId(), results.get(1).getId());
        assertTrue(results.stream().noneMatch(c -> c.getId().equals(foreign.getId())));
    }

    @Test
    void findByIdAndUser_shouldNotReturnAnotherUsersConversation() {

        Long ownerId = createUser();
        Long otherUserId = createUser();

        Conversation conversation = conversationRepository.saveAndFlush(new Conversation(userReference(ownerId), "Private conversation"));

        assertTrue(conversationRepository.findByIdAndUserId(conversation.getId(), ownerId).isPresent());
        assertTrue(conversationRepository.findByIdAndUserId(conversation.getId(), otherUserId).isEmpty());
    }

    @Test
    void findAllMessages_shouldReturnChronologicalOrder() {

        Long userId = createUser();
        Conversation conversation = conversationRepository.saveAndFlush(new Conversation(userReference(userId), "RAG discussion"));

        messageRepository.saveAndFlush(new ChatMessage(conversation, ChatMessageRole.USER, "What is RAG?"));
        messageRepository.saveAndFlush(new ChatMessage(conversation, ChatMessageRole.ASSISTANT, "RAG combines retrieval and generation."));
        messageRepository.saveAndFlush(new ChatMessage(conversation, ChatMessageRole.USER, "How does retrieval work?"));

        entityManager.clear();

        List<ChatMessage> messages = messageRepository.findAllByConversationIdOrderByIdAsc(conversation.getId());

        assertEquals(3, messages.size());
        assertEquals(ChatMessageRole.USER, messages.get(0).getRole());
        assertEquals("What is RAG?", messages.get(0).getContent());
        assertEquals(ChatMessageRole.ASSISTANT, messages.get(1).getRole());
        assertEquals("How does retrieval work?", messages.get(2).getContent());
        assertTrue(messages.get(0).getId() < messages.get(1).getId());
        assertTrue(messages.get(1).getId() < messages.get(2).getId());
    }

    @Test
    void findTop10_shouldReturnOnlyTenNewestMessages() {

        Long userId = createUser();
        Conversation conversation = conversationRepository.saveAndFlush(new Conversation(userReference(userId), "Long conversation"));

        for (int i = 1; i <= 15; i++) {
            messageRepository.save(new ChatMessage(conversation, ChatMessageRole.USER, "Message " + i));
        }

        messageRepository.flush();
        entityManager.clear();

        List<ChatMessage> messages = messageRepository.findTop10ByConversationIdOrderByIdDesc(conversation.getId());

        assertEquals(10, messages.size());
        assertEquals("Message 15", messages.get(0).getContent());
        assertEquals("Message 6", messages.get(9).getContent());

        for (int i = 0; i < messages.size() - 1; i++) {
            assertTrue(messages.get(i).getId() > messages.get(i + 1).getId());
        }
    }

    @Test
    void deleteConversation_shouldCascadeToMessages() {

        Long userId = createUser();
        Conversation conversation = conversationRepository.saveAndFlush(new Conversation(userReference(userId), "Conversation to delete"));

        Long conversationId = conversation.getId();

        messageRepository.saveAndFlush(new ChatMessage(conversation, ChatMessageRole.USER, "First message"));
        messageRepository.saveAndFlush(new ChatMessage(conversation, ChatMessageRole.ASSISTANT, "Second message"));

        Integer beforeDelete = jdbcTemplate.queryForObject(
                """
                        SELECT count(*)
                        FROM chat_messages
                        WHERE conversation_id = ?
                        """,
                Integer.class,
                conversationId
        );

        assertEquals(2, beforeDelete);
        int deleted = jdbcTemplate.update("DELETE FROM conversations WHERE id = ?", conversationId);
        assertEquals(1, deleted);

        Integer remainingMessages = jdbcTemplate.queryForObject(
                """
                        SELECT count(*)
                        FROM chat_messages
                        WHERE conversation_id = ?
                        """,
                Integer.class,
                conversationId
        );

        assertEquals(0, remainingMessages);
        entityManager.clear();
        assertTrue(conversationRepository.findById(conversationId).isEmpty());
    }
}
