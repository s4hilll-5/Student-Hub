package com.studenthub.service;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.studenthub.dto.subject.AttendanceStats;
import com.studenthub.dto.subject.SubjectRequest;
import com.studenthub.dto.subject.SubjectResponse;
import com.studenthub.entity.AttendanceStatus;
import com.studenthub.entity.Subject;
import com.studenthub.entity.User;
import com.studenthub.exception.BadRequestException;
import com.studenthub.exception.ConflictException;
import com.studenthub.exception.ResourceNotFoundException;
import com.studenthub.mapper.SubjectMapper;
import com.studenthub.repository.AttendanceRecordRepository;
import com.studenthub.repository.AttendanceRecordRepository.SubjectStatusCount;
import com.studenthub.repository.SubjectRepository;

@Service
@Transactional(readOnly = true)
public class SubjectService {

    static final String DEFAULT_COLOR = "#6366F1";

    private final SubjectRepository subjectRepository;
    private final AttendanceRecordRepository attendanceRepository;
    private final UserService userService;

    public SubjectService(SubjectRepository subjectRepository, AttendanceRecordRepository attendanceRepository,
            UserService userService) {
        this.subjectRepository = subjectRepository;
        this.attendanceRepository = attendanceRepository;
        this.userService = userService;
    }

    public List<SubjectResponse> list(Long userId, boolean belowTargetOnly) {
        Map<Subject, AttendanceStats> stats = listWithStats(userId);
        return stats.entrySet().stream()
                .filter(entry -> !belowTargetOnly || !entry.getValue().meetsTarget())
                .map(entry -> SubjectMapper.toResponse(entry.getKey(), entry.getValue()))
                .toList();
    }

    public SubjectResponse get(Long userId, Long subjectId) {
        Subject subject = requireOwned(userId, subjectId);
        return SubjectMapper.toResponse(subject, statsFor(subject));
    }

    @Transactional
    public SubjectResponse create(Long userId, SubjectRequest request) {
        User user = userService.requireUser(userId);
        String name = request.name().trim();
        String code = normaliseCode(request.code());
        if (subjectRepository.existsByUserIdAndNameIgnoreCase(userId, name)) {
            throw new ConflictException("You already have a subject called \"" + name + "\".", "name");
        }
        if (code != null && subjectRepository.existsByUserIdAndCodeIgnoreCase(userId, code)) {
            throw new ConflictException("You already have a subject with code " + code + ".", "code");
        }
        Subject subject = new Subject();
        subject.setUser(user);
        apply(subject, request, name, code, user.getAttendanceTarget());
        subjectRepository.save(subject);
        return SubjectMapper.toResponse(subject, statsFor(subject));
    }

    @Transactional
    public SubjectResponse update(Long userId, Long subjectId, SubjectRequest request) {
        Subject subject = requireOwned(userId, subjectId);
        String name = request.name().trim();
        String code = normaliseCode(request.code());
        if (subjectRepository.existsByUserIdAndNameIgnoreCaseAndIdNot(userId, name, subjectId)) {
            throw new ConflictException("You already have a subject called \"" + name + "\".", "name");
        }
        if (code != null && subjectRepository.existsByUserIdAndCodeIgnoreCaseAndIdNot(userId, code, subjectId)) {
            throw new ConflictException("You already have a subject with code " + code + ".", "code");
        }
        apply(subject, request, name, code, subject.getTargetPercentage());
        return SubjectMapper.toResponse(subject, statsFor(subject));
    }

    /**
     * Deletes the subject. The database removes its attendance records and timetable
     * slots, and sets subject_id to NULL on assignments, exams, notes and study sessions.
     */
    @Transactional
    public void delete(Long userId, Long subjectId) {
        Subject subject = requireOwned(userId, subjectId);
        subjectRepository.delete(subject);
    }

    public Subject requireOwned(Long userId, Long subjectId) {
        return subjectRepository.findByIdAndUserId(subjectId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject", subjectId));
    }

    /**
     * Resolves an optional subject reference from a request body. A subject that does not
     * exist, or belongs to someone else, is reported as a field error (400).
     */
    public Subject resolveOptional(Long userId, Long subjectId) {
        if (subjectId == null) {
            return null;
        }
        return subjectRepository.findByIdAndUserId(subjectId, userId)
                .orElseThrow(() -> new BadRequestException("Choose one of your subjects.", "subjectId"));
    }

    /** Every subject of the user with its attendance stats, ordered by name, using two queries in total. */
    public Map<Subject, AttendanceStats> listWithStats(Long userId) {
        List<Subject> subjects = subjectRepository.findByUserIdOrderByNameAsc(userId);
        Map<Long, Map<AttendanceStatus, Long>> counts = groupCounts(attendanceRepository.countByStatusForUser(userId));
        Map<Subject, AttendanceStats> result = new LinkedHashMap<>();
        for (Subject subject : subjects) {
            result.put(subject, calculate(subject, counts.getOrDefault(subject.getId(), Map.of())));
        }
        return result;
    }

    public AttendanceStats statsFor(Subject subject) {
        if (subject.getId() == null) {
            return calculate(subject, Map.of());
        }
        Map<Long, Map<AttendanceStatus, Long>> counts =
                groupCounts(attendanceRepository.countByStatusForSubject(subject.getId()));
        return calculate(subject, counts.getOrDefault(subject.getId(), Map.of()));
    }

    static AttendanceStats calculate(Subject subject, Map<AttendanceStatus, Long> counts) {
        int priorAttended = subject.getPriorAttended() == null ? 0 : subject.getPriorAttended();
        int priorTotal = subject.getPriorTotal() == null ? 0 : subject.getPriorTotal();
        int present = priorAttended + counts.getOrDefault(AttendanceStatus.PRESENT, 0L).intValue();
        int absent = (priorTotal - priorAttended) + counts.getOrDefault(AttendanceStatus.ABSENT, 0L).intValue();
        int cancelled = counts.getOrDefault(AttendanceStatus.CANCELLED, 0L).intValue();
        return AttendanceCalculator.calculate(present, absent, cancelled, subject.getTargetPercentage());
    }

    private static Map<Long, Map<AttendanceStatus, Long>> groupCounts(List<SubjectStatusCount> rows) {
        Map<Long, Map<AttendanceStatus, Long>> grouped = new HashMap<>();
        for (SubjectStatusCount row : rows) {
            grouped.computeIfAbsent(row.getSubjectId(), id -> new EnumMap<>(AttendanceStatus.class))
                    .put(row.getStatus(), row.getTotal());
        }
        return grouped;
    }

    private static void apply(Subject subject, SubjectRequest request, String name, String code, BigDecimal fallbackTarget) {
        subject.setName(name);
        subject.setCode(code);
        subject.setColor(request.color() != null ? request.color().toUpperCase() : (subject.getColor() != null ? subject.getColor() : DEFAULT_COLOR));
        subject.setTargetPercentage(request.targetPercentage() != null ? request.targetPercentage() : fallbackTarget);
        subject.setPriorAttended(request.priorAttended() != null ? request.priorAttended() : 0);
        subject.setPriorTotal(request.priorTotal() != null ? request.priorTotal() : 0);
        if (subject.getPriorAttended() > subject.getPriorTotal()) {
            throw new BadRequestException("Classes attended cannot be more than classes held", "priorAttended");
        }
    }

    private static String normaliseCode(String code) {
        return code == null || code.isBlank() ? null : code.trim().toUpperCase();
    }
}
