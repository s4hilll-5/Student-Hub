package com.studenthub.dto.exam;

import java.time.Instant;

import com.studenthub.entity.ExamType;
import com.studenthub.entity.PreparationStatus;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ExamRequest(
        @NotBlank(message = "Enter the exam name")
        @Size(max = 150, message = "Title must be 150 characters or fewer")
        String title,

        Long subjectId,

        @NotNull(message = "Choose the exam date and time")
        Instant examAt,

        @Min(value = 5, message = "Duration must be between 5 and 600 minutes")
        @Max(value = 600, message = "Duration must be between 5 and 600 minutes")
        Integer durationMinutes,

        @NotNull(message = "Choose the exam type")
        ExamType examType,

        /* Optional; defaults to NOT_STARTED. */
        PreparationStatus preparationStatus,

        @Size(max = 100, message = "Location must be 100 characters or fewer")
        String location,

        @Size(max = 5000, message = "Syllabus notes must be 5000 characters or fewer")
        String syllabusNotes) {
}
