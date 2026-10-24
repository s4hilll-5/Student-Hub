package com.studenthub.dto.subject;

import java.math.BigDecimal;
import java.time.Instant;

public record SubjectResponse(
        Long id,
        String name,
        String code,
        String color,
        BigDecimal targetPercentage,
        int priorAttended,
        int priorTotal,
        AttendanceStats attendance,
        Instant createdAt,
        Instant updatedAt) {
}
