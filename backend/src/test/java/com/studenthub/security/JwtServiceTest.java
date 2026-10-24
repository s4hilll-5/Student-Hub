package com.studenthub.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import com.studenthub.entity.Role;
import com.studenthub.entity.User;
import com.studenthub.support.TestData;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;

class JwtServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-16T10:00:00Z");

    private final JwtService jwtService = new JwtService(TestData.properties(), Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void generatedTokenRoundTrips() {
        User user = TestData.user(42L);
        user.setRole(Role.ADMIN);
        user.setTokenVersion(3);

        JwtService.AccessTokenClaims claims = jwtService.parseAccessToken(jwtService.generateAccessToken(user));

        assertThat(claims.userId()).isEqualTo(42L);
        assertThat(claims.tokenVersion()).isEqualTo(3);
        assertThat(jwtService.getAccessTokenTtlSeconds()).isEqualTo(900);
    }

    @Test
    void tamperedTokenIsRejected() {
        String token = jwtService.generateAccessToken(TestData.user(1L));
        String[] parts = token.split("\\.");
        String forgedPayload = java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"sub\":\"2\",\"iss\":\"studenthub\",\"ver\":0,\"exp\":9999999999}".getBytes());
        String forged = parts[0] + "." + forgedPayload + "." + parts[2];

        assertThatThrownBy(() -> jwtService.parseAccessToken(forged)).isInstanceOf(JwtException.class);
    }

    @Test
    void tokenSignedWithAnotherSecretIsRejected() {
        JwtService other = new JwtService(
                TestData.properties("c29tZS1vdGhlci1zZWNyZXQtdGhhdC1pcy1hdC1sZWFzdC0zMi1ieXRlcy1sb25n"),
                Clock.fixed(NOW, ZoneOffset.UTC));
        String foreign = other.generateAccessToken(TestData.user(1L));

        assertThatThrownBy(() -> jwtService.parseAccessToken(foreign)).isInstanceOf(JwtException.class);
    }

    @Test
    void expiredTokenIsRejected() {
        String token = jwtService.generateAccessToken(TestData.user(1L));
        JwtService later = new JwtService(TestData.properties(),
                Clock.fixed(NOW.plus(Duration.ofMinutes(16)), ZoneOffset.UTC));

        assertThatThrownBy(() -> later.parseAccessToken(token)).isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void missingOrWeakSecretPreventsStartup() {
        assertThatThrownBy(() -> new JwtService(TestData.properties(""), Clock.systemUTC()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
        // "short" in Base64 is far fewer than 32 bytes
        assertThatThrownBy(() -> new JwtService(TestData.properties("c2hvcnQ="), Clock.systemUTC()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 bytes");
    }
}
