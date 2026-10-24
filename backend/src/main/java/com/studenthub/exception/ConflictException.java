package com.studenthub.exception;

/** 409, e.g. duplicate email, duplicate subject name or overlapping timetable slot. */
public class ConflictException extends RuntimeException {

    private final String field;

    public ConflictException(String message) {
        this(message, null);
    }

    public ConflictException(String message, String field) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
