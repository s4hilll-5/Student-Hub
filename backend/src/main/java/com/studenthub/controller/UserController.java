package com.studenthub.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.studenthub.dto.auth.AuthResponse;
import com.studenthub.dto.user.ChangePasswordRequest;
import com.studenthub.dto.user.DeleteAccountRequest;
import com.studenthub.dto.user.UpdateProfileRequest;
import com.studenthub.dto.user.UserResponse;
import com.studenthub.security.AppUserPrincipal;
import com.studenthub.security.RefreshTokenCookieService;
import com.studenthub.service.SessionTokenService.IssuedSession;
import com.studenthub.service.UserService;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

/** The signed-in user's own account. There is no endpoint that accepts another user's id. */
@RestController
@RequestMapping("/api/users/me")
public class UserController {

    private final UserService userService;
    private final RefreshTokenCookieService cookies;

    public UserController(UserService userService, RefreshTokenCookieService cookies) {
        this.userService = userService;
        this.cookies = cookies;
    }

    @GetMapping
    public UserResponse me(@AuthenticationPrincipal AppUserPrincipal principal) {
        return userService.getProfile(principal.getId());
    }

    @PutMapping
    public UserResponse update(@AuthenticationPrincipal AppUserPrincipal principal,
            @Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateProfile(principal.getId(), request);
    }

    /** Signs out all other devices and returns a fresh session for this one. */
    @PutMapping("/password")
    public AuthResponse changePassword(@AuthenticationPrincipal AppUserPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest request, HttpServletResponse response) {
        IssuedSession session = userService.changePassword(principal.getId(), request);
        cookies.write(response, session.refreshToken());
        return session.response();
    }

    /** Body: { "password": "...", "confirmation": "DELETE" }. 204 on success. */
    @DeleteMapping
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AppUserPrincipal principal,
            @Valid @RequestBody DeleteAccountRequest request, HttpServletResponse response) {
        userService.deleteAccount(principal.getId(), request);
        cookies.clear(response);
        return ResponseEntity.noContent().build();
    }
}
