package com.studenthub.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Service;

import com.studenthub.config.AppProperties;
import com.studenthub.entity.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

/**
 * Issues and verifies short-lived, HMAC-signed access tokens.
 *
 * Claims: sub = user id, role, ver = token version, iss, iat, exp, jti.
 * The secret comes from JWT_SECRET (Base64, at least 256 bits); the app refuses to
 * start without one, so there is no insecure fallback key.
 */
@Service
public class JwtService {

    static final String CLAIM_ROLE = "role";
    static final String CLAIM_VERSION = "ver";

    private final SecretKey key;
    private final String issuer;
    private final Duration accessTokenTtl;
    private final Clock clock;

    public JwtService(AppProperties properties, Clock clock) {
        AppProperties.Jwt jwt = properties.jwt();
        if (jwt == null || jwt.secret() == null || jwt.secret().isBlank()) {
            throw new IllegalStateException(
                    "JWT_SECRET is not set. Generate one with: openssl rand -base64 64");
        }
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(jwt.secret().trim());
        } catch (RuntimeException e) {
            throw new IllegalStateException("JWT_SECRET must be Base64-encoded (openssl rand -base64 64)", e);
        }
        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET must decode to at least 32 bytes (256 bits)");
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.issuer = jwt.issuer();
        this.accessTokenTtl = jwt.accessTokenTtl();
        this.clock = clock;
    }

    public String generateAccessToken(User user) {
        Instant now = clock.instant();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .issuer(issuer)
                .subject(String.valueOf(user.getId()))
                .claim(CLAIM_ROLE, user.getRole().name())
                .claim(CLAIM_VERSION, user.getTokenVersion())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTokenTtl)))
                .signWith(key)
                .compact();
    }

    /**
     * Verifies signature, issuer and expiry.
     *
     * @throws JwtException             if the token is invalid, tampered with or expired
     * @throws IllegalArgumentException if the token is blank or its claims are malformed
     */
    public AccessTokenClaims parseAccessToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .requireIssuer(issuer)
                .clock(() -> Date.from(clock.instant()))
                .clockSkewSeconds(30)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        Integer version = claims.get(CLAIM_VERSION, Integer.class);
        if (claims.getSubject() == null || version == null) {
            throw new IllegalArgumentException("Token is missing required claims");
        }
        return new AccessTokenClaims(Long.valueOf(claims.getSubject()), version);
    }

    public long getAccessTokenTtlSeconds() {
        return accessTokenTtl.toSeconds();
    }

    public record AccessTokenClaims(Long userId, int tokenVersion) {
    }
}
