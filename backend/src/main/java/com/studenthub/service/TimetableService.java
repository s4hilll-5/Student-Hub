package com.studenthub.service;

import java.time.DayOfWeek;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.studenthub.dto.timetable.TimetableRequest;
import com.studenthub.dto.timetable.TimetableResponse;
import com.studenthub.entity.TimetableEntry;
import com.studenthub.exception.ConflictException;
import com.studenthub.exception.ResourceNotFoundException;
import com.studenthub.mapper.ScheduleMapper;
import com.studenthub.repository.TimetableEntryRepository;
import com.studenthub.repository.UserRepository;

@Service
@Transactional(readOnly = true)
public class TimetableService {

    private static final Comparator<TimetableEntry> WEEK_ORDER = Comparator
            .comparing(TimetableEntry::getDayOfWeek)
            .thenComparing(TimetableEntry::getStartTime);

    private final TimetableEntryRepository timetableRepository;
    private final UserRepository userRepository;
    private final SubjectService subjectService;

    public TimetableService(TimetableEntryRepository timetableRepository, UserRepository userRepository,
            SubjectService subjectService) {
        this.timetableRepository = timetableRepository;
        this.userRepository = userRepository;
        this.subjectService = subjectService;
    }

    /** The whole week (Monday first), or one day when {@code day} is given. */
    public List<TimetableResponse> list(Long userId, DayOfWeek day) {
        List<TimetableEntry> entries = day == null
                ? timetableRepository.findByUserId(userId)
                : timetableRepository.findByUserIdAndDayOfWeekOrderByStartTimeAsc(userId, day);
        return entries.stream().sorted(WEEK_ORDER).map(ScheduleMapper::toResponse).toList();
    }

    @Transactional
    public TimetableResponse create(Long userId, TimetableRequest request) {
        ensureNoOverlap(userId, request, -1L);
        TimetableEntry entry = new TimetableEntry();
        entry.setUser(userRepository.getReferenceById(userId));
        apply(userId, entry, request);
        timetableRepository.save(entry);
        return ScheduleMapper.toResponse(entry);
    }

    @Transactional
    public TimetableResponse update(Long userId, Long id, TimetableRequest request) {
        TimetableEntry entry = requireOwned(userId, id);
        ensureNoOverlap(userId, request, id);
        apply(userId, entry, request);
        return ScheduleMapper.toResponse(entry);
    }

    @Transactional
    public void delete(Long userId, Long id) {
        timetableRepository.delete(requireOwned(userId, id));
    }

    private TimetableEntry requireOwned(Long userId, Long id) {
        return timetableRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Class", id));
    }

    private void apply(Long userId, TimetableEntry entry, TimetableRequest request) {
        // Required here (not optional): a class slot always belongs to a subject.
        entry.setSubject(subjectService.requireOwned(userId, request.subjectId()));
        entry.setDayOfWeek(request.dayOfWeek());
        entry.setStartTime(request.startTime());
        entry.setEndTime(request.endTime());
        entry.setLocation(UserService.blankToNull(request.location()));
    }

    private void ensureNoOverlap(Long userId, TimetableRequest request, Long excludeId) {
        List<TimetableEntry> clashes = timetableRepository.findOverlapping(
                userId, request.dayOfWeek(), request.startTime(), request.endTime(), excludeId);
        if (!clashes.isEmpty()) {
            TimetableEntry clash = clashes.get(0);
            throw new ConflictException("This overlaps with " + clash.getSubject().getName() + " on "
                    + TimeUtils.dayName(clash.getDayOfWeek()) + " ("
                    + TimeUtils.formatRange(clash.getStartTime(), clash.getEndTime()) + ").", "startTime");
        }
    }
}
