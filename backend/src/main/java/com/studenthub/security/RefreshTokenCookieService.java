package com.studenthub.security;

import java.time.Duration;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import com.studenthub.config.AppProperties;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Writes and clears the refresh-token cookie.
 *
 * HttpOnly: JavaScript (and therefore XSS) cannot read it.
 * Secure: only sent over HTTPS (disable only for local HTTP development).
 * SameSite=Strict: not sent on cross-site requests, which is the first CSRF defence.
 * Path=/api/auth: not sent to any other endpoint.
 */
@Component
public class RefreshTokenCookieService {

    public static final String COOKIE_NAME = "studenthub_refresh";
    public static final String COOKIE_PATH = "/api/auth";

    private final AppProperties properties;

    public RefreshTokenCookieService(AppProperties properties) {
        this.properties = properties;
    }

    public void write(HttpServletResponse response, String rawToken) {
        response.addHeader(HttpHeaders.SET_COOKIE,
                build(rawToken, properties.jwt().refreshTokenTtl()).toString());
    }

    public void clear(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, build("", Duration.ZERO).toString());
    }

    private ResponseCookie build(String value, Duration maxAge) {
        return ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)
                .secure(properties.cookie().secure())
                .sameSite(properties.cookie().sameSite())
                .path(COOKIE_PATH)
                .maxAge(maxAge)
                .build();
    }
}
