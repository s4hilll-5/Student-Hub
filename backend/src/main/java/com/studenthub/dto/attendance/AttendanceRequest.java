package com.studenthub.dto.attendance;

import java.time.LocalDate;

import com.studenthub.entity.AttendanceStatus;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * One class outcome. The "not in the future" rule is checked in the service using the
 * student's own timezone, because "today" in Mumbai is not "today" on a UTC server.
 */
public record AttendanceRequest(
        @NotNull(message = "Choose the class date")
        LocalDate classDate,

        @NotNull(message = "Choose present, absent or cancelled")
        AttendanceStatus status,

        @Size(max = 255, message = "Notes must be 255 characters or fewer")
        String notes) {
}
