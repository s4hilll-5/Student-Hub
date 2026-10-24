package com.studenthub.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.studenthub.dto.attendance.AttendanceRequest;
import com.studenthub.dto.attendance.AttendanceResponse;
import com.studenthub.dto.common.PageResponse;
import com.studenthub.entity.AttendanceRecord;
import com.studenthub.entity.Subject;
import com.studenthub.entity.User;
import com.studenthub.exception.BadRequestException;
import com.studenthub.exception.ResourceNotFoundException;
import com.studenthub.mapper.AttendanceMapper;
import com.studenthub.repository.AttendanceRecordRepository;

@Service
@Transactional(readOnly = true)
public class AttendanceService {

    static final LocalDate EARLIEST_CLASS_DATE = LocalDate.of(2000, 1, 1);
    private static final Set<String> SORTABLE = Set.of("classDate", "createdAt", "status");

    private final AttendanceRecordRepository attendanceRepository;
    private final SubjectService subjectService;
    private final UserService userService;
    private final Clock clock;

    public AttendanceService(AttendanceRecordRepository attendanceRepository, SubjectService subjectService,
            UserService userService, Clock clock) {
        this.attendanceRepository = attendanceRepository;
        this.subjectService = subjectService;
        this.userService = userService;
        this.clock = clock;
    }

    public PageResponse<AttendanceResponse> list(Long userId, Long subjectId, LocalDate from, LocalDate to, Pageable pageable) {
        subjectService.requireOwned(userId, subjectId);
        if (from != null && to != null && to.isBefore(from)) {
            throw new BadRequestException("'to' must be on or after 'from'", "to");
        }
        Pageable page = PageRequests.sanitize(pageable, SORTABLE, Sort.by(Sort.Direction.DESC, "classDate"));
        Page<AttendanceRecord> records = (from == null && to == null)
                ? attendanceRepository.findBySubjectId(subjectId, page)
                : attendanceRepository.findBySubjectIdAndClassDateBetween(subjectId,
                        from != null ? from : EARLIEST_CLASS_DATE,
                        to != null ? to : LocalDate.of(9999, 12, 31), page);
        return PageResponse.of(records, AttendanceMapper::toResponse);
    }

    @Transactional
    public AttendanceResponse create(Long userId, Long subjectId, AttendanceRequest request) {
        Subject subject = subjectService.requireOwned(userId, subjectId);
        validateDate(userId, request.classDate());
        AttendanceRecord record = new AttendanceRecord();
        record.setSubject(subject);
        apply(record, request);
        attendanceRepository.save(record);
        return AttendanceMapper.toResponse(record);
    }

    @Transactional
    public AttendanceResponse update(Long userId, Long recordId, AttendanceRequest request) {
        AttendanceRecord record = requireOwned(userId, recordId);
        validateDate(userId, request.classDate());
        apply(record, request);
        return AttendanceMapper.toResponse(record);
    }

    @Transactional
    public void delete(Long userId, Long recordId) {
        attendanceRepository.delete(requireOwned(userId, recordId));
    }

    private AttendanceRecord requireOwned(Long userId, Long recordId) {
        return attendanceRepository.findByIdAndSubjectUserId(recordId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance record", recordId));
    }

    /** A class can't be recorded for a day that hasn't happened yet in the student's own timezone. */
    private void validateDate(Long userId, LocalDate classDate) {
        User user = userService.requireUser(userId);
        LocalDate today = TimeUtils.today(clock, TimeUtils.zoneOf(user));
        if (classDate.isAfter(today)) {
            throw new BadRequestException("You can't record attendance for a future date.", "classDate");
        }
        if (classDate.isBefore(EARLIEST_CLASS_DATE)) {
            throw new BadRequestException("Choose a date after 1 January 2000.", "classDate");
        }
    }

    private static void apply(AttendanceRecord record, AttendanceRequest request) {
        record.setClassDate(request.classDate());
        record.setStatus(request.status());
        record.setNotes(UserService.blankToNull(request.notes()));
    }
}
