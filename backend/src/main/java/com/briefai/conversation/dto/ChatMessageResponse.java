package com.briefai.conversation.dto;

import com.briefai.conversation.entity.ChatMessageRole;

import java.time.LocalDateTime;
import java.util.List;

public class ChatMessageResponse {
    private Long id;
    private ChatMessageRole role;
    private String content;
    private LocalDateTime createdAt;

    private List<ChatMessageSourceResponse> sources;

    public ChatMessageResponse(Long id, ChatMessageRole role, String content, LocalDateTime createdAt, List<ChatMessageSourceResponse> sources) {
        this.id = id;
        this.role = role;
        this.content = content;
        this.createdAt = createdAt;
        this.sources = sources;
    }

    public List<ChatMessageSourceResponse> getSources() {
        return sources;
    }

    public void setSources(List<ChatMessageSourceResponse> sources) {
        this.sources = sources;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ChatMessageRole getRole() {
        return role;
    }

    public void setRole(ChatMessageRole role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
