package com.briefai.document.retrieval.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class DocumentSearchRequest {
    @NotBlank(message = "Question is required.")
    @Size(max = 2000, message = "Question must not exceed 2000 characters.")
    private String question;

    public DocumentSearchRequest(String question) {
        this.question = question;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }
}
