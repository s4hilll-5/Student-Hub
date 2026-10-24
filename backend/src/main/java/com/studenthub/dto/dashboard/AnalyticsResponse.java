package com.studenthub.dto.dashboard;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.studenthub.dto.common.SubjectSummary;
import com.studenthub.entity.AssignmentStatus;

/**
 * Aggregated data for the analytics page. Weekly series always contain every week in
 * the range (zero-filled), so charts never have gaps and the frontend does no maths.
 */
public record AnalyticsResponse(
        int weeks,
        AssignmentAnalytics assignments,
        List<SubjectAttendance> subjectAttendance,
        List<WeeklyStudy> weeklyStudy,
        StudyProgress studyGoal,
        List<DeadlineItem> upcomingDeadlines) {

    /** completionRate is completed / total x 100 (1 decimal), or null when there are no assignments. */
    public record AssignmentAnalytics(
            long total,
            long completed,
            long open,
            long overdue,
            BigDecimal completionRate,
            List<StatusCount> byStatus,
            List<WeeklyAssignments> weeklyTrend) {
    }

    public record StatusCount(AssignmentStatus status, long count) {
    }

    public record WeeklyAssignments(LocalDate weekStart, long created, long completed) {
    }

    public record SubjectAttendance(
            Long subjectId,
            String name,
            String code,
            String color,
            BigDecimal percentage,
            BigDecimal target,
            boolean meetsTarget,
            int present,
            int totalCounted) {
    }

    public record WeeklyStudy(
            LocalDate weekStart,
            BigDecimal hours,
            long minutes,
            long completedSessions,
            long plannedSessions,
            int goalHours) {
    }

    /** type is "ASSIGNMENT" or "EXAM". */
    public record DeadlineItem(String type, Long id, String title, SubjectSummary subject, Instant dueAt) {
    }
}
