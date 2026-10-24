package com.studenthub.dto.study;

import java.time.LocalDate;
import java.time.LocalTime;

import com.studenthub.validation.TimeRanged;
import com.studenthub.validation.ValidTimeRange;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@ValidTimeRange
public record StudySessionRequest(
        @NotBlank(message = "Enter what you'll study")
        @Size(max = 150, message = "Title must be 150 characters or fewer")
        String title,

        Long subjectId,

        @NotNull(message = "Choose a date")
        LocalDate sessionDate,

        @NotNull(message = "Choose a start time")
        LocalTime startTime,

        @NotNull(message = "Choose an end time")
        LocalTime endTime,

        @Size(max = 500, message = "Notes must be 500 characters or fewer")
        String notes) implements TimeRanged {
}
