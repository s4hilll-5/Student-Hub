package com.studenthub.dto.admin;

import jakarta.validation.constraints.NotNull;

public record UserStatusRequest(@NotNull(message = "Say whether the account is enabled") Boolean enabled) {
}
