package com.studenthub.dto.attendance;

import java.time.Instant;
import java.time.LocalDate;

import com.studenthub.entity.AttendanceStatus;

public record AttendanceResponse(
        Long id,
        Long subjectId,
        LocalDate classDate,
        AttendanceStatus status,
        String notes,
        Instant createdAt,
        Instant updatedAt) {
}
