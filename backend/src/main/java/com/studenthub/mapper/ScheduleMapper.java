package com.studenthub.mapper;

import com.studenthub.dto.study.StudySessionResponse;
import com.studenthub.dto.timetable.TimetableResponse;
import com.studenthub.entity.StudySession;
import com.studenthub.entity.TimetableEntry;

/** Maps timetable slots and study sessions. */
public final class ScheduleMapper {

    private ScheduleMapper() {
    }

    public static TimetableResponse toResponse(TimetableEntry entry) {
        return new TimetableResponse(
                entry.getId(),
                SubjectMapper.toSummary(entry.getSubject()),
                entry.getDayOfWeek(),
                entry.getStartTime(),
                entry.getEndTime(),
                entry.getLocation(),
                entry.getCreatedAt(),
                entry.getUpdatedAt());
    }

    public static StudySessionResponse toResponse(StudySession session) {
        return new StudySessionResponse(
                session.getId(),
                session.getTitle(),
                SubjectMapper.toSummary(session.getSubject()),
                session.getSessionDate(),
                session.getStartTime(),
                session.getEndTime(),
                session.getPlannedMinutes(),
                session.isCompleted(),
                session.getActualMinutes(),
                session.getNotes(),
                session.getCompletedAt(),
                session.getCreatedAt(),
                session.getUpdatedAt());
    }
}
