package com.studenthub.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Account deletion needs the password and the word DELETE typed out. */
public record DeleteAccountRequest(
        @NotBlank(message = "Enter your password")
        @Size(max = 200)
        String password,

        @NotBlank(message = "Type DELETE to confirm")
        @Pattern(regexp = "DELETE", message = "Type DELETE to confirm")
        String confirmation) {
}
