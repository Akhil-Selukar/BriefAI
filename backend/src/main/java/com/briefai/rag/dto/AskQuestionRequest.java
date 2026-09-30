package com.briefai.rag.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AskQuestionRequest {
    @NotBlank
    @Size(max = 2000, message = "Question must not exceed 2000 characters.")
    private String question;

    public AskQuestionRequest(String question) {
        this.question = question;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }
}
