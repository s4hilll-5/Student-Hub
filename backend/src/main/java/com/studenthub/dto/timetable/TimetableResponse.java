package com.studenthub.dto.timetable;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;

import com.studenthub.dto.common.SubjectSummary;

public record TimetableResponse(
        Long id,
        SubjectSummary subject,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        String location,
        Instant createdAt,
        Instant updatedAt) {
}
