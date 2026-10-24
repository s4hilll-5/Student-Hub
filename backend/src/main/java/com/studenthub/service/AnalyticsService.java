package com.studenthub.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.studenthub.dto.dashboard.AnalyticsResponse;
import com.studenthub.dto.dashboard.AnalyticsResponse.AssignmentAnalytics;
import com.studenthub.dto.dashboard.AnalyticsResponse.DeadlineItem;
import com.studenthub.dto.dashboard.AnalyticsResponse.StatusCount;
import com.studenthub.dto.dashboard.AnalyticsResponse.SubjectAttendance;
import com.studenthub.dto.dashboard.AnalyticsResponse.WeeklyAssignments;
import com.studenthub.dto.dashboard.AnalyticsResponse.WeeklyStudy;
import com.studenthub.dto.dashboard.DashboardSummary.AssignmentCounts;
import com.studenthub.dto.subject.AttendanceStats;
import com.studenthub.entity.AssignmentStatus;
import com.studenthub.entity.Subject;
import com.studenthub.entity.User;
import com.studenthub.mapper.SubjectMapper;
import com.studenthub.repository.AssignmentRepository;
import com.studenthub.repository.ExaminationRepository;
import com.studenthub.repository.SessionDay;
import com.studenthub.repository.StudySessionRepository;

/**
 * Reporting queries for the analytics page. Rows are fetched for a bounded window
 * (at most 26 weeks) and bucketed into weeks in the student's timezone here, so the
 * frontend only draws what it receives.
 */
@Service
@Transactional(readOnly = true)
public class AnalyticsService {

    public static final int DEFAULT_WEEKS = 8;
    public static final int MAX_WEEKS = 26;
    static final Duration DEADLINE_HORIZON = Duration.ofDays(14);
    static final int MAX_DEADLINES = 10;

    private final UserService userService;
    private final SubjectService subjectService;
    private final DashboardService dashboardService;
    private final AssignmentRepository assignmentRepository;
    private final ExaminationRepository examRepository;
    private final StudySessionRepository studySessionRepository;
    private final Clock clock;

    public AnalyticsService(UserService userService, SubjectService subjectService, DashboardService dashboardService,
            AssignmentRepository assignmentRepository, ExaminationRepository examRepository,
            StudySessionRepository studySessionRepository, Clock clock) {
        this.userService = userService;
        this.subjectService = subjectService;
        this.dashboardService = dashboardService;
        this.assignmentRepository = assignmentRepository;
        this.examRepository = examRepository;
        this.studySessionRepository = studySessionRepository;
        this.clock = clock;
    }

    public AnalyticsResponse analytics(Long userId, int requestedWeeks) {
        int weeks = Math.max(1, Math.min(requestedWeeks, MAX_WEEKS));
        User user = userService.requireUser(userId);
        ZoneId zone = TimeUtils.zoneOf(user);
        Instant now = clock.instant();
        LocalDate currentWeek = TimeUtils.weekStart(TimeUtils.today(clock, zone));
        LocalDate firstWeek = currentWeek.minusWeeks(weeks - 1L);

        return new AnalyticsResponse(
                weeks,
                assignmentAnalytics(userId, zone, now, firstWeek, currentWeek),
                subjectAttendance(userId),
                weeklyStudy(user, firstWeek, currentWeek),
                dashboardService.studyProgress(user, currentWeek),
                upcomingDeadlines(userId, now));
    }

    private AssignmentAnalytics assignmentAnalytics(Long userId, ZoneId zone, Instant now,
            LocalDate firstWeek, LocalDate currentWeek) {
        AssignmentCounts counts = dashboardService.assignmentCounts(userId, now);
        BigDecimal completionRate = counts.total() == 0
                ? null
                : BigDecimal.valueOf(counts.completed()).multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(counts.total()), 1, RoundingMode.HALF_UP);
        List<StatusCount> byStatus = List.of(
                new StatusCount(AssignmentStatus.PENDING, counts.pending()),
                new StatusCount(AssignmentStatus.IN_PROGRESS, counts.inProgress()),
                new StatusCount(AssignmentStatus.COMPLETED, counts.completed()));

        Instant rangeStart = firstWeek.atStartOfDay(zone).toInstant();
        Map<LocalDate, Long> created = countByWeek(assignmentRepository.findCreatedAtSince(userId, rangeStart), zone);
        Map<LocalDate, Long> completed = countByWeek(assignmentRepository.findCompletedAtSince(userId, rangeStart), zone);
        List<WeeklyAssignments> trend = new ArrayList<>();
        for (LocalDate week = firstWeek; !week.isAfter(currentWeek); week = week.plusWeeks(1)) {
            trend.add(new WeeklyAssignments(week, created.getOrDefault(week, 0L), completed.getOrDefault(week, 0L)));
        }
        long open = counts.pending() + counts.inProgress();
        return new AssignmentAnalytics(counts.total(), counts.completed(), open, counts.overdue(), completionRate,
                byStatus, trend);
    }

    private List<SubjectAttendance> subjectAttendance(Long userId) {
        List<SubjectAttendance> result = new ArrayList<>();
        for (Map.Entry<Subject, AttendanceStats> entry : subjectService.listWithStats(userId).entrySet()) {
            Subject s = entry.getKey();
            AttendanceStats stats = entry.getValue();
            result.add(new SubjectAttendance(s.getId(), s.getName(), s.getCode(), s.getColor(), stats.percentage(),
                    stats.target(), stats.meetsTarget(), stats.present(), stats.totalCounted()));
        }
        return result;
    }

    private List<WeeklyStudy> weeklyStudy(User user, LocalDate firstWeek, LocalDate currentWeek) {
        List<SessionDay> sessions = studySessionRepository.findSessionDays(
                user.getId(), firstWeek, currentWeek.plusDays(6));
        Map<LocalDate, long[]> buckets = new HashMap<>(); // [minutes, completed, planned]
        for (SessionDay session : sessions) {
            long[] bucket = buckets.computeIfAbsent(TimeUtils.weekStart(session.sessionDate()), w -> new long[3]);
            bucket[2]++;
            if (Boolean.TRUE.equals(session.completed())) {
                bucket[1]++;
                bucket[0] += session.actualMinutes() == null ? 0 : session.actualMinutes();
            }
        }
        List<WeeklyStudy> result = new ArrayList<>();
        for (LocalDate week = firstWeek; !week.isAfter(currentWeek); week = week.plusWeeks(1)) {
            long[] bucket = buckets.getOrDefault(week, new long[3]);
            result.add(new WeeklyStudy(week, DashboardService.toHours(bucket[0]), bucket[0], bucket[1], bucket[2],
                    user.getWeeklyStudyGoalHours()));
        }
        return result;
    }

    private List<DeadlineItem> upcomingDeadlines(Long userId, Instant now) {
        Instant horizon = now.plus(DEADLINE_HORIZON);
        Stream<DeadlineItem> assignments = assignmentRepository
                .findByUserIdAndStatusNotAndDueAtBetweenOrderByDueAtAsc(userId, AssignmentStatus.COMPLETED, now, horizon)
                .stream()
                .map(a -> new DeadlineItem("ASSIGNMENT", a.getId(), a.getTitle(), SubjectMapper.toSummary(a.getSubject()),
                        a.getDueAt()));
        Stream<DeadlineItem> exams = examRepository.findByUserIdAndExamAtBetweenOrderByExamAtAsc(userId, now, horizon)
                .stream()
                .map(e -> new DeadlineItem("EXAM", e.getId(), e.getTitle(), SubjectMapper.toSummary(e.getSubject()),
                        e.getExamAt()));
        return Stream.concat(assignments, exams)
                .sorted(Comparator.comparing(DeadlineItem::dueAt))
                .limit(MAX_DEADLINES)
                .toList();
    }

    private static Map<LocalDate, Long> countByWeek(List<Instant> instants, ZoneId zone) {
        Map<LocalDate, Long> counts = new HashMap<>();
        for (Instant instant : instants) {
            if (instant != null) {
                counts.merge(TimeUtils.weekStart(LocalDate.ofInstant(instant, zone)), 1L, Long::sum);
            }
        }
        return counts;
    }
}
