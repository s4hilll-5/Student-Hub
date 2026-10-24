package com.studenthub.dto.note;

import jakarta.validation.constraints.NotNull;

public record NotePinRequest(@NotNull(message = "Say whether the note is pinned") Boolean pinned) {
}
