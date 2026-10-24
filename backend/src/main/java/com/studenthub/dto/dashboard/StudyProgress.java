package com.studenthub.dto.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Study time for one Monday-Sunday week in the student's timezone.
 * goalProgressPercent can exceed 100 when the goal is beaten.
 */
public record StudyProgress(
        LocalDate weekStart,
        LocalDate weekEnd,
        long minutesThisWeek,
        BigDecimal hoursThisWeek,
        int goalHours,
        BigDecimal goalProgressPercent,
        long completedSessions,
        long plannedSessions) {
}
