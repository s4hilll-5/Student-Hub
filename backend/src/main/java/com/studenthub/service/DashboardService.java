package com.studenthub.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.studenthub.dto.dashboard.DashboardSummary;
import com.studenthub.dto.dashboard.DashboardSummary.AssignmentCounts;
import com.studenthub.dto.dashboard.DashboardSummary.AttendanceOverview;
import com.studenthub.dto.dashboard.StudyProgress;
import com.studenthub.dto.subject.AttendanceStats;
import com.studenthub.entity.AssignmentStatus;
import com.studenthub.entity.Subject;
import com.studenthub.entity.User;
import com.studenthub.mapper.AssignmentMapper;
import com.studenthub.mapper.ExamMapper;
import com.studenthub.mapper.ScheduleMapper;
import com.studenthub.repository.AssignmentRepository;
import com.studenthub.repository.ExaminationRepository;
import com.studenthub.repository.StudySessionRepository;
import com.studenthub.repository.TimetableEntryRepository;

/** Builds the dashboard from live database queries. Nothing here is cached or hard-coded. */
@Service
@Transactional(readOnly = true)
public class DashboardService {

    private static final BigDecimal SIXTY = BigDecimal.valueOf(60);
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final UserService userService;
    private final SubjectService subjectService;
    private final AssignmentRepository assignmentRepository;
    private final ExaminationRepository examRepository;
    private final TimetableEntryRepository timetableRepository;
    private final StudySessionRepository studySessionRepository;
    private final Clock clock;

    public DashboardService(UserService userService, SubjectService subjectService,
            AssignmentRepository assignmentRepository, ExaminationRepository examRepository,
            TimetableEntryRepository timetableRepository, StudySessionRepository studySessionRepository, Clock clock) {
        this.userService = userService;
        this.subjectService = subjectService;
        this.assignmentRepository = assignmentRepository;
        this.examRepository = examRepository;
        this.timetableRepository = timetableRepository;
        this.studySessionRepository = studySessionRepository;
        this.clock = clock;
    }

    public DashboardSummary summary(Long userId) {
        User user = userService.requireUser(userId);
        ZoneId zone = TimeUtils.zoneOf(user);
        Instant now = clock.instant();
        LocalDate today = TimeUtils.today(clock, zone);

        return new DashboardSummary(
                firstName(user.getFullName()),
                today,
                zone.getId(),
                assignmentCounts(userId, now),
                attendanceOverview(user),
                studyProgress(user, TimeUtils.weekStart(today)),
                timetableRepository.findByUserIdAndDayOfWeekOrderByStartTimeAsc(userId, today.getDayOfWeek())
                        .stream().map(ScheduleMapper::toResponse).toList(),
                studySessionRepository.findByUserIdAndSessionDateOrderByStartTimeAsc(userId, today)
                        .stream().map(ScheduleMapper::toResponse).toList(),
                examRepository.findTop5ByUserIdAndExamAtGreaterThanEqualOrderByExamAtAsc(userId, now)
                        .stream().map(exam -> ExamMapper.toResponse(exam, now)).toList(),
                assignmentRepository.findTop5ByUserIdAndStatusNotAndDueAtGreaterThanEqualOrderByDueAtAsc(
                                userId, AssignmentStatus.COMPLETED, now)
                        .stream().map(a -> AssignmentMapper.toResponse(a, now)).toList());
    }

    AssignmentCounts assignmentCounts(Long userId, Instant now) {
        return new AssignmentCounts(
                assignmentRepository.countByUserId(userId),
                assignmentRepository.countByUserIdAndStatus(userId, AssignmentStatus.PENDING),
                assignmentRepository.countByUserIdAndStatus(userId, AssignmentStatus.IN_PROGRESS),
                assignmentRepository.countByUserIdAndStatus(userId, AssignmentStatus.COMPLETED),
                assignmentRepository.countByUserIdAndStatusNotAndDueAtBefore(userId, AssignmentStatus.COMPLETED, now));
    }

    /** Overall attendance pools every subject's classes and compares against the profile target. */
    AttendanceOverview attendanceOverview(User user) {
        Map<Subject, AttendanceStats> stats = subjectService.listWithStats(user.getId());
        int present = 0;
        int absent = 0;
        int cancelled = 0;
        int below = 0;
        for (AttendanceStats s : stats.values()) {
            present += s.present();
            absent += s.absent();
            cancelled += s.cancelled();
            if (!s.meetsTarget()) {
                below++;
            }
        }
        AttendanceStats overall = AttendanceCalculator.calculate(present, absent, cancelled, user.getAttendanceTarget());
        return new AttendanceOverview(overall.percentage(), user.getAttendanceTarget(), overall.present(),
                overall.totalCounted(), stats.size(), below);
    }

    /** Study minutes for the Monday-Sunday week starting at weekStart. */
    StudyProgress studyProgress(User user, LocalDate weekStart) {
        LocalDate weekEnd = weekStart.plusDays(6);
        Long minutesValue = studySessionRepository.sumCompletedMinutes(user.getId(), weekStart, weekEnd);
        long minutes = minutesValue == null ? 0 : minutesValue;
        int goalHours = user.getWeeklyStudyGoalHours();
        BigDecimal hours = toHours(minutes);
        BigDecimal progress = goalHours <= 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(minutes).multiply(HUNDRED)
                        .divide(BigDecimal.valueOf(goalHours * 60L), 1, RoundingMode.HALF_UP);
        long completed = studySessionRepository.countByUserIdAndCompletedTrueAndSessionDateBetween(user.getId(), weekStart, weekEnd);
        long planned = studySessionRepository.countByUserIdAndSessionDateBetween(user.getId(), weekStart, weekEnd);
        return new StudyProgress(weekStart, weekEnd, minutes, hours, goalHours, progress, completed, planned);
    }

    static BigDecimal toHours(long minutes) {
        return BigDecimal.valueOf(minutes).divide(SIXTY, 1, RoundingMode.HALF_UP);
    }

    static String firstName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "there";
        }
        return fullName.trim().split("\\s+")[0];
    }
}
