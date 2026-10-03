package com.briefai.conversation.controller;

import com.briefai.auth.dto.AuthenticatedUser;
import com.briefai.conversation.dto.ChatMessageResponse;
import com.briefai.conversation.dto.ConversationResponse;
import com.briefai.conversation.dto.CreateConversationRequest;
import com.briefai.conversation.service.ConversationService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/conversations")
public class ConversationController {

    private static final Logger logger = LoggerFactory.getLogger(ConversationController.class);
    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @PostMapping
    public ResponseEntity<ConversationResponse> create(@AuthenticationPrincipal AuthenticatedUser user, @Valid @RequestBody CreateConversationRequest request) {
        logger.debug("Request received to create a new conversation");
        ConversationResponse response = conversationService.create(user.getId(), request.getTitle());
        logger.debug("Conversation {} created successfully", response.getTitle());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public List<ConversationResponse> listAll(@AuthenticationPrincipal AuthenticatedUser user) {
        logger.debug("Request received to fetch all conversations created by user {}", user.getEmail());
        return conversationService.list(user.getId());
    }

    @GetMapping("/{conversationId}")
    public ConversationResponse getById(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long conversationId) {
        logger.debug("Request received to fetch conversation by id");
        return conversationService.get(user.getId(), conversationId);
    }

    @GetMapping("/{conversationId}/messages")
    public List<ChatMessageResponse> getMessages(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long conversationId) {
        logger.debug("Request received to fetch messages from conversation with id {}", conversationId);
        return conversationService.getMessages(user.getId(), conversationId);
    }

    @DeleteMapping("/{conversationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long conversationId) {
        logger.debug("Request received to delete the conversation with id {}", conversationId);
        conversationService.delete(user.getId(), conversationId);
    }
}
