package com.studenthub.dto.subject;

import java.math.BigDecimal;

import com.studenthub.validation.ValidAttendanceCounts;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@ValidAttendanceCounts
public record SubjectRequest(
        @NotBlank(message = "Enter a subject name")
        @Size(max = 100, message = "Name must be 100 characters or fewer")
        String name,

        @Size(max = 20, message = "Code must be 20 characters or fewer")
        @Pattern(regexp = "^[A-Za-z0-9 _./-]*$", message = "Code can use letters, numbers, spaces and - _ . /")
        String code,

        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Colour must be a hex value like #6366F1")
        String color,

        /* Optional; defaults to the attendance target on the student's profile. */
        @DecimalMin(value = "1.00", message = "Target must be between 1 and 100")
        @DecimalMax(value = "100.00", message = "Target must be between 1 and 100")
        @Digits(integer = 3, fraction = 2, message = "Use at most 2 decimal places")
        BigDecimal targetPercentage,

        /* Classes attended / held before tracking started in StudentHub. Default 0. */
        @Min(value = 0, message = "Classes attended cannot be negative")
        @Max(value = 1000, message = "Classes attended looks too high")
        Integer priorAttended,

        @Min(value = 0, message = "Classes held cannot be negative")
        @Max(value = 1000, message = "Classes held looks too high")
        Integer priorTotal) {
}
