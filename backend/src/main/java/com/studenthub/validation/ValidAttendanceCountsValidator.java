package com.studenthub.validation;

import com.studenthub.dto.subject.SubjectRequest;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidAttendanceCountsValidator implements ConstraintValidator<ValidAttendanceCounts, SubjectRequest> {

    @Override
    public boolean isValid(SubjectRequest value, ConstraintValidatorContext context) {
        if (value == null || value.priorAttended() == null || value.priorTotal() == null) {
            return true;
        }
        if (value.priorAttended() <= value.priorTotal()) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                .addPropertyNode("priorAttended")
                .addConstraintViolation();
        return false;
    }
}
