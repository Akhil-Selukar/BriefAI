package com.briefai.conversation.repository;

import com.briefai.conversation.entity.ChatMessageSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageSourceRepository extends JpaRepository<ChatMessageSource, Long> {
    List<ChatMessageSource> findAllByMessageIdOrderBySourceNumberAsc(Long messageId);
}