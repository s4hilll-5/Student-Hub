package com.studenthub.dto.user;

import com.studenthub.validation.StrongPassword;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "Enter your current password")
        @Size(max = 200)
        String currentPassword,

        @StrongPassword
        String newPassword) {
}
