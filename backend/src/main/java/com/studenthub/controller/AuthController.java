package com.studenthub.controller;

import java.time.Clock;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.studenthub.dto.auth.AuthResponse;
import com.studenthub.dto.auth.ForgotPasswordRequest;
import com.studenthub.dto.auth.LoginRequest;
import com.studenthub.dto.auth.RegisterRequest;
import com.studenthub.dto.auth.ResetPasswordRequest;
import com.studenthub.dto.common.ApiError;
import com.studenthub.dto.common.MessageResponse;
import com.studenthub.exception.UnauthorizedException;
import com.studenthub.security.RefreshTokenCookieService;
import com.studenthub.service.AuthService;
import com.studenthub.service.SessionTokenService.IssuedSession;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    static final String RESET_REQUESTED =
            "If an account exists for that email, we've sent a link to reset the password.";

    private final AuthService authService;
    private final RefreshTokenCookieService cookies;
    private final Clock clock;

    public AuthController(AuthService authService, RefreshTokenCookieService cookies, Clock clock) {
        this.authService = authService;
        this.cookies = cookies;
        this.clock = clock;
    }

    /** 201 + access token in the body + refresh token in an HttpOnly cookie. 409 if the email is taken. */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request, HttpServletResponse response) {
        IssuedSession session = authService.register(request);
        cookies.write(response, session.refreshToken());
        return ResponseEntity.status(HttpStatus.CREATED).body(session.response());
    }

    /** 200 on success, 401 "Invalid email or password." otherwise. */
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        IssuedSession session = authService.login(request);
        cookies.write(response, session.refreshToken());
        return session.response();
    }

    /**
     * Exchanges the refresh cookie for a new access token and a rotated cookie.
     * Requires the X-XSRF-TOKEN header (CSRF protection for cookie-authenticated endpoints).
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(
            @CookieValue(name = RefreshTokenCookieService.COOKIE_NAME, required = false) String refreshToken,
            HttpServletRequest request, HttpServletResponse response) {
        try {
            IssuedSession session = authService.refresh(refreshToken);
            cookies.write(response, session.refreshToken());
            return ResponseEntity.ok(session.response());
        } catch (UnauthorizedException e) {
            // Clear the dead cookie in the same response that reports the failure.
            cookies.clear(response);
            ApiError body = new ApiError(clock.instant(), 401, HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                    e.getMessage(), request.getRequestURI(), List.of());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
        }
    }

    /** Revokes the refresh token and clears the cookie. Always 204. */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = RefreshTokenCookieService.COOKIE_NAME, required = false) String refreshToken,
            HttpServletResponse response) {
        authService.logout(refreshToken);
        cookies.clear(response);
        return ResponseEntity.noContent().build();
    }

    /**
     * Issues the XSRF-TOKEN cookie (the CSRF filter writes it on every response that lacks
     * one). The SPA calls this once before its first refresh/logout.
     */
    @GetMapping("/csrf")
    public ResponseEntity<Void> csrf() {
        return ResponseEntity.noContent().build();
    }

    /** Always 202 with the same message, so it cannot be used to find out which emails are registered. */
    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.requestPasswordReset(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new MessageResponse(RESET_REQUESTED));
    }

    @PostMapping("/reset-password")
    public MessageResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return new MessageResponse("Your password has been changed. Sign in with your new password.");
    }
}
