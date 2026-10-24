package com.studenthub.dto.timetable;

import java.time.DayOfWeek;
import java.time.LocalTime;

import com.studenthub.validation.TimeRanged;
import com.studenthub.validation.ValidTimeRange;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@ValidTimeRange
public record TimetableRequest(
        @NotNull(message = "Choose a subject")
        Long subjectId,

        @NotNull(message = "Choose a day")
        DayOfWeek dayOfWeek,

        @NotNull(message = "Choose a start time")
        LocalTime startTime,

        @NotNull(message = "Choose an end time")
        LocalTime endTime,

        @Size(max = 100, message = "Location must be 100 characters or fewer")
        String location) implements TimeRanged {
}
