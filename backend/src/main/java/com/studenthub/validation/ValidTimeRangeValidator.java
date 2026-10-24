package com.studenthub.validation;

import java.time.Duration;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidTimeRangeValidator implements ConstraintValidator<ValidTimeRange, TimeRanged> {

    static final long MIN_MINUTES = 5;

    @Override
    public boolean isValid(TimeRanged value, ConstraintValidatorContext context) {
        if (value == null || value.startTime() == null || value.endTime() == null) {
            // @NotNull on the individual fields reports missing values.
            return true;
        }
        String message = null;
        if (!value.endTime().isAfter(value.startTime())) {
            message = "End time must be after start time";
        } else if (Duration.between(value.startTime(), value.endTime()).toMinutes() < MIN_MINUTES) {
            message = "A slot must last at least " + MIN_MINUTES + " minutes";
        }
        if (message == null) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message)
                .addPropertyNode("endTime")
                .addConstraintViolation();
        return false;
    }
}
