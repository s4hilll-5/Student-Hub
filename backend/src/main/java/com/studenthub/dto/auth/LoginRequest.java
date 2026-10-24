package com.studenthub.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "Enter your email")
        @Email(message = "Enter a valid email address")
        @Size(max = 254)
        String email,

        @NotBlank(message = "Enter your password")
        @Size(max = 200)
        String password) {
}
