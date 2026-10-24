package com.studenthub.mapper;

import com.studenthub.dto.common.SubjectSummary;
import com.studenthub.dto.subject.AttendanceStats;
import com.studenthub.dto.subject.SubjectResponse;
import com.studenthub.entity.Subject;

public final class SubjectMapper {

    private SubjectMapper() {
    }

    /** Null-safe: assignments, exams, notes and study sessions may have no subject. */
    public static SubjectSummary toSummary(Subject subject) {
        if (subject == null) {
            return null;
        }
        return new SubjectSummary(subject.getId(), subject.getName(), subject.getCode(), subject.getColor());
    }

    public static SubjectResponse toResponse(Subject subject, AttendanceStats stats) {
        return new SubjectResponse(
                subject.getId(),
                subject.getName(),
                subject.getCode(),
                subject.getColor(),
                subject.getTargetPercentage(),
                subject.getPriorAttended(),
                subject.getPriorTotal(),
                stats,
                subject.getCreatedAt(),
                subject.getUpdatedAt());
    }
}
