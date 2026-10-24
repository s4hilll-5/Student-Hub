package com.studenthub.dto.study;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import com.studenthub.dto.common.SubjectSummary;

public record StudySessionResponse(
        Long id,
        String title,
        SubjectSummary subject,
        LocalDate sessionDate,
        LocalTime startTime,
        LocalTime endTime,
        int plannedMinutes,
        boolean completed,
        Integer actualMinutes,
        String notes,
        Instant completedAt,
        Instant createdAt,
        Instant updatedAt) {
}
