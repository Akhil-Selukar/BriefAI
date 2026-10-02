package com.briefai.conversation.repository;

import com.briefai.conversation.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findAllByConversationIdOrderByIdAsc(Long conversationId);

    List<ChatMessage> findTop10ByConversationIdOrderByIdDesc(Long conversationId);
}
