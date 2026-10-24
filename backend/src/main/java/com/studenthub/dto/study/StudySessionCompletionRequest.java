package com.studenthub.dto.study;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Marks a session completed (with the minutes actually studied) or re-opens it.
 * actualMinutes is required when completed is true.
 */
public record StudySessionCompletionRequest(
        @NotNull(message = "Say whether the session is completed")
        Boolean completed,

        @Min(value = 1, message = "Actual time must be between 1 and 1440 minutes")
        @Max(value = 1440, message = "Actual time must be between 1 and 1440 minutes")
        Integer actualMinutes) {
}
