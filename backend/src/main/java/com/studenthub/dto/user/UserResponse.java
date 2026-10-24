package com.studenthub.dto.user;

import java.math.BigDecimal;
import java.time.Instant;

import com.studenthub.entity.Role;

/** Public view of an account. Deliberately has no password hash or token version. */
public record UserResponse(
        Long id,
        String fullName,
        String email,
        Role role,
        String college,
        String course,
        Integer semester,
        String timezone,
        BigDecimal attendanceTarget,
        Integer weeklyStudyGoalHours,
        Instant createdAt) {
}
