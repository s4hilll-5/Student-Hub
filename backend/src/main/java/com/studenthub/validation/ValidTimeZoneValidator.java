package com.studenthub.validation;

import java.time.ZoneId;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidTimeZoneValidator implements ConstraintValidator<ValidTimeZone, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        if (value.isBlank() || value.length() > 64) {
            return false;
        }
        return ZoneId.getAvailableZoneIds().contains(value) || "UTC".equals(value);
    }
}
