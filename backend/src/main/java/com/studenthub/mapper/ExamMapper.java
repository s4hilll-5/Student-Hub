package com.studenthub.mapper;

import java.time.Instant;

import com.studenthub.dto.exam.ExamResponse;
import com.studenthub.entity.Examination;

public final class ExamMapper {

    private ExamMapper() {
    }

    public static ExamResponse toResponse(Examination exam, Instant now) {
        return new ExamResponse(
                exam.getId(),
                exam.getTitle(),
                SubjectMapper.toSummary(exam.getSubject()),
                exam.getExamAt(),
                exam.getDurationMinutes(),
                exam.getExamType(),
                exam.getPreparationStatus(),
                exam.getLocation(),
                exam.getSyllabusNotes(),
                !exam.getExamAt().isBefore(now),
                exam.getCreatedAt(),
                exam.getUpdatedAt());
    }
}
