package com.studenthub.dto.assignment;

import java.time.Instant;

import com.studenthub.entity.AssignmentStatus;
import com.studenthub.entity.Priority;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Create/update payload. There is intentionally no userId field: the owner is always
 * the authenticated user taken from the security context.
 */
public record AssignmentRequest(
        @NotBlank(message = "Enter a title")
        @Size(max = 150, message = "Title must be 150 characters or fewer")
        String title,

        @Size(max = 5000, message = "Description must be 5000 characters or fewer")
        String description,

        /* Optional; must be one of the caller's own subjects. */
        Long subjectId,

        /* ISO-8601 instant, e.g. 2026-10-20T18:29:00Z (the browser converts local time to UTC). */
        @NotNull(message = "Choose a due date and time")
        Instant dueAt,

        @NotNull(message = "Choose a priority")
        Priority priority,

        /* Optional on create (defaults to PENDING); on update, null keeps the current status. */
        AssignmentStatus status) {
}
