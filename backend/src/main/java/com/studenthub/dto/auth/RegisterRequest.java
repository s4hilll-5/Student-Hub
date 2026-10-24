package com.studenthub.dto.auth;

import com.studenthub.validation.StrongPassword;
import com.studenthub.validation.ValidTimeZone;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Enter your name")
        @Size(max = 100, message = "Name must be 100 characters or fewer")
        String fullName,

        @NotBlank(message = "Enter your email")
        @Email(message = "Enter a valid email address")
        @Size(max = 254, message = "Email is too long")
        String email,

        @StrongPassword
        String password,

        /* Browser timezone, e.g. "Asia/Kolkata". Optional; defaults to UTC. */
        @ValidTimeZone
        String timezone) {
}
