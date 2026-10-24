package com.studenthub.dto.auth;

import com.studenthub.validation.StrongPassword;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank(message = "The reset link is missing its token")
        @Size(max = 200)
        String token,

        @StrongPassword
        String newPassword) {
}
