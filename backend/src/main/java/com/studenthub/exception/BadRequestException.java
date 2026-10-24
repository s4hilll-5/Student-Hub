package com.studenthub.exception;

/** 400 for business-rule validation that Bean Validation annotations cannot express. */
public class BadRequestException extends RuntimeException {

    private final String field;

    public BadRequestException(String message) {
        this(message, null);
    }

    public BadRequestException(String message, String field) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
