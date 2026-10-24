package com.studenthub.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/** Class-level check on SubjectRequest: classes attended can never exceed classes held. */
@Documented
@Constraint(validatedBy = ValidAttendanceCountsValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidAttendanceCounts {

    String message() default "Classes attended cannot be more than classes held";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
