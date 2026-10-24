package com.studenthub.dto.exam;

import java.time.Instant;

import com.studenthub.dto.common.SubjectSummary;
import com.studenthub.entity.ExamType;
import com.studenthub.entity.PreparationStatus;

public record ExamResponse(
        Long id,
        String title,
        SubjectSummary subject,
        Instant examAt,
        Integer durationMinutes,
        ExamType examType,
        PreparationStatus preparationStatus,
        String location,
        String syllabusNotes,
        boolean upcoming,
        Instant createdAt,
        Instant updatedAt) {
}
