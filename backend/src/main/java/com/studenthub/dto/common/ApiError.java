package com.studenthub.dto.common;

import java.time.Instant;
import java.util.List;

/**
 * The single error shape returned by every endpoint, including security failures:
 * <pre>
 * { "timestamp": "...", "status": 400, "error": "Bad Request",
 *   "message": "Some fields need attention.", "path": "/api/assignments",
 *   "fieldErrors": [ { "field": "title", "message": "Title is required" } ] }
 * </pre>
 * Stack traces and internal exception messages are never included.
 */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldViolation> fieldErrors) {

    public record FieldViolation(String field, String message) {
    }
}
