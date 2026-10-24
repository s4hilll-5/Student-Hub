package com.studenthub.dto.user;

import java.math.BigDecimal;

import com.studenthub.validation.ValidTimeZone;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Email is not editable here: it is the login identity and would need re-verification. */
public record UpdateProfileRequest(
        @NotBlank(message = "Enter your name")
        @Size(max = 100, message = "Name must be 100 characters or fewer")
        String fullName,

        @Size(max = 150, message = "College must be 150 characters or fewer")
        String college,

        @Size(max = 100, message = "Course must be 100 characters or fewer")
        String course,

        @Min(value = 1, message = "Semester must be between 1 and 12")
        @Max(value = 12, message = "Semester must be between 1 and 12")
        Integer semester,

        @NotBlank(message = "Choose a timezone")
        @ValidTimeZone
        String timezone,

        @NotNull(message = "Set an attendance target")
        @DecimalMin(value = "1.00", message = "Attendance target must be between 1 and 100")
        @DecimalMax(value = "100.00", message = "Attendance target must be between 1 and 100")
        @Digits(integer = 3, fraction = 2, message = "Use at most 2 decimal places")
        BigDecimal attendanceTarget,

        @NotNull(message = "Set a weekly study goal")
        @Min(value = 1, message = "Weekly study goal must be between 1 and 112 hours")
        @Max(value = 112, message = "Weekly study goal must be between 1 and 112 hours")
        Integer weeklyStudyGoalHours) {
}
