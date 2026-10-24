package com.studenthub.config;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Type-safe view of the {@code app.*} settings in application.yml.
 * Values come from environment variables; see backend/.env.example.
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        @DefaultValue("http://localhost:5173") String frontendUrl,
        Jwt jwt,
        @DefaultValue Cookie cookie,
        @DefaultValue Cors cors,
        @DefaultValue Security security,
        @DefaultValue PasswordReset passwordReset,
        @DefaultValue Mail mail,
        @DefaultValue Admin admin,
        @DefaultValue RateLimit rateLimit) {

    public record Jwt(
            String secret,
            @DefaultValue("studenthub") String issuer,
            @DefaultValue("15m") Duration accessTokenTtl,
            @DefaultValue("7d") Duration refreshTokenTtl,
            @DefaultValue("20s") Duration refreshReuseGrace) {
    }

    public record Cookie(
            @DefaultValue("true") boolean secure,
            @DefaultValue("Strict") String sameSite) {
    }

    public record Cors(@DefaultValue("http://localhost:5173") List<String> allowedOrigins) {
    }

    public record Security(@DefaultValue("12") int bcryptStrength) {
    }

    public record PasswordReset(@DefaultValue("30m") Duration tokenTtl) {
    }

    public record Mail(
            @DefaultValue("false") boolean enabled,
            @DefaultValue("StudentHub <no-reply@studenthub.local>") String from) {
    }

    public record Admin(String email, String password) {
    }

    public record RateLimit(@DefaultValue("true") boolean enabled) {
    }
}
