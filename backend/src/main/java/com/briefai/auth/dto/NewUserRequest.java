package com.briefai.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class NewUserRequest {

    @NotBlank(message = "Name of user is required")
    private String name;
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Password must not be blank.")
    @Size(min = 8, max = 20, message = "Password length must be between 8 to 20 characters.")
    @Pattern(regexp = ".*[a-z].*", message = "Password must contains at-least 1 lowercase character.")
    @Pattern(regexp = ".*[A-Z].*", message = "Password must contains at-least 1 uppercase character.")
    @Pattern(regexp = ".*\\d.*", message = "Password must contains at-least 1 digit.")
    @Pattern(regexp = ".*[!@#$].*", message = "Password must contains at-least 1 special character (allowed are !, @, # and $).")
    @Pattern(regexp = "^[A-Za-z\\d!@#$]+$", message = " only allowed characters are 'A-Z', 'a-z', '0-9, !, @, # and $.")
    private String password;

    public NewUserRequest(String name, String email, String password) {
        this.name = name;
        this.email = email;
        this.password = password;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }
}
