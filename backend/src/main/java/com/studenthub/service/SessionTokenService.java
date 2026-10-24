package com.studenthub.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.studenthub.dto.auth.AuthResponse;
import com.studenthub.entity.User;
import com.studenthub.mapper.UserMapper;
import com.studenthub.security.JwtService;

/** Issues the access-token + refresh-token pair for a signed-in user. */
@Service
public class SessionTokenService {

    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public SessionTokenService(JwtService jwtService, RefreshTokenService refreshTokenService) {
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    /** The response body plus the raw refresh token the controller puts into the cookie. */
    public record IssuedSession(AuthResponse response, String refreshToken) {
    }

    @Transactional
    public IssuedSession issue(User user) {
        String refreshToken = refreshTokenService.create(user);
        return withRefreshToken(user, refreshToken);
    }

    public IssuedSession withRefreshToken(User user, String refreshToken) {
        String accessToken = jwtService.generateAccessToken(user);
        AuthResponse body = AuthResponse.bearer(accessToken, jwtService.getAccessTokenTtlSeconds(), UserMapper.toResponse(user));
        return new IssuedSession(body, refreshToken);
    }
}
