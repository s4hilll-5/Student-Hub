package com.studenthub.dto.assignment;

import java.time.Instant;

import com.studenthub.dto.common.SubjectSummary;
import com.studenthub.entity.AssignmentStatus;
import com.studenthub.entity.Priority;

public record AssignmentResponse(
        Long id,
        String title,
        String description,
        SubjectSummary subject,
        Instant dueAt,
        Priority priority,
        AssignmentStatus status,
        boolean overdue,
        Instant completedAt,
        Instant createdAt,
        Instant updatedAt) {
}
