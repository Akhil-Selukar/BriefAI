package com.briefai.auth.dto;

public class NewUserResponse {
    private Long id;
    private String name;
    private String email;
    private boolean emailVerified;

    public NewUserResponse(Long id, String name, String email, boolean emailVerified) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.emailVerified = emailVerified;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }
}
