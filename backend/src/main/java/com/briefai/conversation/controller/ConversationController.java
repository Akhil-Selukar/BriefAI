package com.briefai.conversation.controller;

import com.briefai.auth.dto.AuthenticatedUser;
import com.briefai.conversation.dto.ChatMessageResponse;
import com.briefai.conversation.dto.ConversationResponse;
import com.briefai.conversation.dto.CreateConversationRequest;
import com.briefai.conversation.service.ConversationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/conversations")
public class ConversationController {

    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @PostMapping
    public ResponseEntity<ConversationResponse> create(@AuthenticationPrincipal AuthenticatedUser user, @Valid @RequestBody CreateConversationRequest request) {
        ConversationResponse response = conversationService.create(user.getId(), request.getTitle());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public List<ConversationResponse> listAll(@AuthenticationPrincipal AuthenticatedUser user) {
        return conversationService.list(user.getId());
    }

    @GetMapping("/{conversationId}")
    public ConversationResponse getById(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long conversationId) {
        return conversationService.get(user.getId(), conversationId);
    }

    @GetMapping("/{conversationId}/messages")
    public List<ChatMessageResponse> getMessages(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long conversationId) {
        return conversationService.getMessages(user.getId(), conversationId);
    }

    @DeleteMapping("/{conversationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long conversationId) {
        conversationService.delete(user.getId(), conversationId);
    }
}
