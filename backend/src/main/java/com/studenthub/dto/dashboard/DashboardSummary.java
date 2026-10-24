package com.studenthub.dto.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.studenthub.dto.assignment.AssignmentResponse;
import com.studenthub.dto.exam.ExamResponse;
import com.studenthub.dto.study.StudySessionResponse;
import com.studenthub.dto.timetable.TimetableResponse;

/** Everything the dashboard needs in one request. Every number is computed from the database. */
public record DashboardSummary(
        String firstName,
        LocalDate today,
        String timezone,
        AssignmentCounts assignments,
        AttendanceOverview attendance,
        StudyProgress study,
        List<TimetableResponse> todaysClasses,
        List<StudySessionResponse> todaysStudySessions,
        List<ExamResponse> upcomingExams,
        List<AssignmentResponse> upcomingDeadlines) {

    /** pending = PENDING only; open = pending + inProgress. */
    public record AssignmentCounts(long total, long pending, long inProgress, long completed, long overdue) {
    }

    /** overallPercentage is null until at least one class has been counted. */
    public record AttendanceOverview(
            BigDecimal overallPercentage,
            BigDecimal target,
            int present,
            int totalCounted,
            int subjects,
            int subjectsBelowTarget) {
    }
}
