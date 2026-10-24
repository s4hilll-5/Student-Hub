package com.studenthub.dto.assignment;

import com.studenthub.entity.AssignmentStatus;

import jakarta.validation.constraints.NotNull;

public record AssignmentStatusRequest(@NotNull(message = "Choose a status") AssignmentStatus status) {
}
