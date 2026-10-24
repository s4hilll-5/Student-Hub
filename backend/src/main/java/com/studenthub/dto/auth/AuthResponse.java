package com.studenthub.dto.auth;

import com.studenthub.dto.user.UserResponse;

/**
 * Returned by register, login, refresh and change-password. The refresh token is
 * never in this body: it travels only in the HttpOnly cookie.
 */
public record AuthResponse(String accessToken, String tokenType, long expiresIn, UserResponse user) {

    public static AuthResponse bearer(String accessToken, long expiresInSeconds, UserResponse user) {
        return new AuthResponse(accessToken, "Bearer", expiresInSeconds, user);
    }
}
