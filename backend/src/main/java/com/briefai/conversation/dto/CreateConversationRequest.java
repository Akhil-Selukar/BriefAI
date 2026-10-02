package com.briefai.conversation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateConversationRequest {
    @NotBlank(message = "Title is required.")
    @Size(max = 200)
    private String title;

    public CreateConversationRequest(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
