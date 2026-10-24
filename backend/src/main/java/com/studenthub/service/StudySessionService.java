package com.studenthub.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.studenthub.dto.study.StudySessionCompletionRequest;
import com.studenthub.dto.study.StudySessionRequest;
import com.studenthub.dto.study.StudySessionResponse;
import com.studenthub.entity.StudySession;
import com.studenthub.entity.TimetableEntry;
import com.studenthub.entity.User;
import com.studenthub.exception.BadRequestException;
import com.studenthub.exception.ConflictException;
import com.studenthub.exception.ResourceNotFoundException;
import com.studenthub.mapper.ScheduleMapper;
import com.studenthub.repository.StudySessionRepository;
import com.studenthub.repository.TimetableEntryRepository;

@Service
@Transactional(readOnly = true)
public class StudySessionService {

    static final int MAX_RANGE_DAYS = 366;

    private final StudySessionRepository sessionRepository;
    private final TimetableEntryRepository timetableRepository;
    private final SubjectService subjectService;
    private final UserService userService;
    private final Clock clock;

    public StudySessionService(StudySessionRepository sessionRepository, TimetableEntryRepository timetableRepository,
            SubjectService subjectService, UserService userService, Clock clock) {
        this.sessionRepository = sessionRepository;
        this.timetableRepository = timetableRepository;
        this.subjectService = subjectService;
        this.userService = userService;
        this.clock = clock;
    }

    /** Sessions between two dates (inclusive). Defaults to the current Monday-Sunday week. */
    public List<StudySessionResponse> list(Long userId, LocalDate from, LocalDate to) {
        User user = userService.requireUser(userId);
        LocalDate weekStart = TimeUtils.weekStart(TimeUtils.today(clock, TimeUtils.zoneOf(user)));
        LocalDate start = from != null ? from : weekStart;
        LocalDate end = to != null ? to : start.plusDays(6);
        if (end.isBefore(start)) {
            throw new BadRequestException("'to' must be on or after 'from'", "to");
        }
        if (ChronoUnit.DAYS.between(start, end) > MAX_RANGE_DAYS) {
            throw new BadRequestException("Choose a range of at most " + MAX_RANGE_DAYS + " days", "to");
        }
        return sessionRepository.findByUserIdAndSessionDateBetweenOrderBySessionDateAscStartTimeAsc(userId, start, end)
                .stream().map(ScheduleMapper::toResponse).toList();
    }

    @Transactional
    public StudySessionResponse create(Long userId, StudySessionRequest request) {
        User user = userService.requireUser(userId);
        ensureNoClash(userId, request, -1L);
        StudySession session = new StudySession();
        session.setUser(user);
        apply(userId, session, request);
        sessionRepository.save(session);
        return ScheduleMapper.toResponse(session);
    }

    @Transactional
    public StudySessionResponse update(Long userId, Long id, StudySessionRequest request) {
        StudySession session = requireOwned(userId, id);
        ensureNoClash(userId, request, id);
        apply(userId, session, request);
        return ScheduleMapper.toResponse(session);
    }

    @Transactional
    public StudySessionResponse setCompletion(Long userId, Long id, StudySessionCompletionRequest request) {
        StudySession session = requireOwned(userId, id);
        if (Boolean.TRUE.equals(request.completed())) {
            User user = userService.requireUser(userId);
            LocalDate today = TimeUtils.today(clock, TimeUtils.zoneOf(user));
            if (session.getSessionDate().isAfter(today)) {
                throw new BadRequestException("You can't complete a session that hasn't happened yet.", "completed");
            }
            int minutes = request.actualMinutes() != null ? request.actualMinutes() : session.getPlannedMinutes();
            session.setCompleted(true);
            session.setActualMinutes(minutes);
            session.setCompletedAt(clock.instant());
        } else {
            session.setCompleted(false);
            session.setActualMinutes(null);
            session.setCompletedAt(null);
        }
        return ScheduleMapper.toResponse(session);
    }

    @Transactional
    public void delete(Long userId, Long id) {
        sessionRepository.delete(requireOwned(userId, id));
    }

    private StudySession requireOwned(Long userId, Long id) {
        return sessionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Study session", id));
    }

    private void apply(Long userId, StudySession session, StudySessionRequest request) {
        session.setTitle(request.title().trim());
        session.setSubject(subjectService.resolveOptional(userId, request.subjectId()));
        session.setSessionDate(request.sessionDate());
        session.setStartTime(request.startTime());
        session.setEndTime(request.endTime());
        session.setNotes(UserService.blankToNull(request.notes()));
    }

    /** A study session may not overlap another session, or a class on the same weekday. */
    private void ensureNoClash(Long userId, StudySessionRequest request, Long excludeId) {
        List<StudySession> sessions = sessionRepository.findOverlapping(
                userId, request.sessionDate(), request.startTime(), request.endTime(), excludeId);
        if (!sessions.isEmpty()) {
            StudySession clash = sessions.get(0);
            throw new ConflictException("This overlaps with your study session \"" + clash.getTitle() + "\" ("
                    + TimeUtils.formatRange(clash.getStartTime(), clash.getEndTime()) + ").", "startTime");
        }
        List<TimetableEntry> classes = timetableRepository.findOverlapping(
                userId, request.sessionDate().getDayOfWeek(), request.startTime(), request.endTime(), -1L);
        if (!classes.isEmpty()) {
            TimetableEntry clash = classes.get(0);
            throw new ConflictException("This clashes with your " + clash.getSubject().getName() + " class ("
                    + TimeUtils.formatRange(clash.getStartTime(), clash.getEndTime()) + ").", "startTime");
        }
    }
}
