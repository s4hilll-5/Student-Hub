package com.studenthub.dto.assignment;

import java.time.Instant;

import com.studenthub.entity.AssignmentStatus;
import com.studenthub.entity.Priority;

/** Optional query filters for GET /api/assignments. Every field may be null. */
public record AssignmentFilter(
        String search,
        Long subjectId,
        Priority priority,
        AssignmentStatus status,
        Boolean overdue,
        Instant dueFrom,
        Instant dueTo) {
}
