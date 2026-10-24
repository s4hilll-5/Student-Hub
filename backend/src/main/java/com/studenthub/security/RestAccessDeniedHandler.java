package com.studenthub.security;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** 403 as JSON: wrong role (e.g. a student calling /api/admin/**) or a missing/invalid CSRF token. */
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final JsonErrorWriter errorWriter;

    public RestAccessDeniedHandler(JsonErrorWriter errorWriter) {
        this.errorWriter = errorWriter;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex)
            throws IOException {
        String message = ex instanceof CsrfException
                ? "Security check failed. Refresh the page and try again."
                : "You don't have permission to do that.";
        errorWriter.write(request, response, HttpStatus.FORBIDDEN, message);
    }
}
