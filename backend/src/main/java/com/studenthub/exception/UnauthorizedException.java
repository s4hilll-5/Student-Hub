package com.studenthub.exception;

/** 401 for failures that happen inside controllers, e.g. a missing or invalid refresh cookie. */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
