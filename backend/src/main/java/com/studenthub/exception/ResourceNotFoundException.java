package com.studenthub.exception;

/**
 * 404. Also used when a record exists but belongs to another user: answering 404
 * instead of 403 avoids confirming that someone else's record id exists.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resource, Object id) {
        super(resource + " not found" + (id != null ? " (id " + id + ")" : ""));
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
