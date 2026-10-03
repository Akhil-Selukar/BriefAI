package com.briefai.rag.controller;

import com.briefai.auth.dto.AuthenticatedUser;
import com.briefai.email.service.SmtpEmailService;
import com.briefai.rag.dto.AskQuestionRequest;
import com.briefai.rag.dto.RagResponse;
import com.briefai.rag.service.RagService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {

    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    private final RagService ragService;

    public ChatController(RagService ragService) {
        this.ragService = ragService;
    }

    @PostMapping("/ask")
    public RagResponse ask(@AuthenticationPrincipal AuthenticatedUser user, @Valid @RequestBody AskQuestionRequest request) {
        logger.debug("Received question from user");
        if (request.getConversationId() == null) {
            return ragService.answer(user.getId(), request.getQuestion());
        }

        return ragService.answer(user.getId(), request.getConversationId(), request.getQuestion());
    }
}
