package com.studenthub.mapper;

import java.time.Instant;

import com.studenthub.dto.assignment.AssignmentResponse;
import com.studenthub.entity.Assignment;

public final class AssignmentMapper {

    private AssignmentMapper() {
    }

    /** "now" is passed in so the overdue flag uses the same clock as the rest of the request. */
    public static AssignmentResponse toResponse(Assignment assignment, Instant now) {
        return new AssignmentResponse(
                assignment.getId(),
                assignment.getTitle(),
                assignment.getDescription(),
                SubjectMapper.toSummary(assignment.getSubject()),
                assignment.getDueAt(),
                assignment.getPriority(),
                assignment.getStatus(),
                assignment.isOverdue(now),
                assignment.getCompletedAt(),
                assignment.getCreatedAt(),
                assignment.getUpdatedAt());
    }
}
