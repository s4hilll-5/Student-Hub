package com.studenthub.service;

import java.time.Clock;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.studenthub.config.AppProperties;
import com.studenthub.entity.RefreshToken;
import com.studenthub.entity.RevokeReason;
import com.studenthub.entity.User;
import com.studenthub.exception.UnauthorizedException;
import com.studenthub.repository.RefreshTokenRepository;
import com.studenthub.security.TokenHasher;

/**
 * Refresh-token rotation with reuse detection.
 *
 * Every refresh revokes the presented token and issues a new one. If an already-rotated
 * token is presented again later, it was probably stolen and replayed, so every session
 * of that user is revoked. A short grace window tolerates two browser tabs refreshing at
 * the same instant.
 */
@Service
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);
    private static final String SESSION_ENDED = "Your session has ended. Sign in again.";

    private final RefreshTokenRepository repository;
    private final AppProperties properties;
    private final Clock clock;

    public RefreshTokenService(RefreshTokenRepository repository, AppProperties properties, Clock clock) {
        this.repository = repository;
        this.properties = properties;
        this.clock = clock;
    }

    public record Rotation(User user, String newRawToken) {
    }

    /** Stores the hash of a new token and returns the raw value for the cookie. */
    @Transactional
    public String create(User user) {
        String raw = TokenHasher.newToken();
        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setTokenHash(TokenHasher.sha256(raw));
        token.setExpiresAt(clock.instant().plus(properties.jwt().refreshTokenTtl()));
        repository.save(token);
        return raw;
    }

    /** noRollbackFor: a detected reuse must still commit the revocation before answering 401. */
    @Transactional(noRollbackFor = UnauthorizedException.class)
    public Rotation rotate(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new UnauthorizedException("No active session. Sign in to continue.");
        }
        RefreshToken token = repository.findByTokenHash(TokenHasher.sha256(raw))
                .orElseThrow(() -> new UnauthorizedException(SESSION_ENDED));
        Instant now = clock.instant();
        User user = token.getUser();

        if (token.isRevoked()) {
            boolean concurrentRefresh = token.getRevokedReason() == RevokeReason.ROTATED
                    && token.getRevokedAt().plus(properties.jwt().refreshReuseGrace()).isAfter(now);
            if (!concurrentRefresh) {
                repository.revokeAllForUser(user.getId(), now, RevokeReason.REUSE_DETECTED);
                log.warn("Refresh token reuse detected for user id {}; all sessions revoked", user.getId());
                throw new UnauthorizedException(SESSION_ENDED);
            }
        } else if (token.isExpired(now)) {
            throw new UnauthorizedException("Your session has expired. Sign in again.");
        } else {
            token.revoke(now, RevokeReason.ROTATED);
        }

        if (!user.isEnabled()) {
            throw new UnauthorizedException(SESSION_ENDED);
        }
        return new Rotation(user, create(user));
    }

    @Transactional
    public void revoke(String raw, RevokeReason reason) {
        if (raw == null || raw.isBlank()) {
            return;
        }
        repository.findByTokenHash(TokenHasher.sha256(raw))
                .ifPresent(token -> token.revoke(clock.instant(), reason));
    }

    @Transactional
    public void revokeAll(Long userId, RevokeReason reason) {
        repository.revokeAllForUser(userId, clock.instant(), reason);
    }
}
