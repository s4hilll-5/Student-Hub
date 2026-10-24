package com.studenthub.service;

import java.time.Clock;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.studenthub.repository.PasswordResetTokenRepository;
import com.studenthub.repository.RefreshTokenRepository;

/** Nightly removal of expired refresh tokens and used/expired reset tokens. */
@Component
public class TokenCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(TokenCleanupJob.class);

    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final Clock clock;

    public TokenCleanupJob(RefreshTokenRepository refreshTokenRepository,
            PasswordResetTokenRepository resetTokenRepository, Clock clock) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.resetTokenRepository = resetTokenRepository;
        this.clock = clock;
    }

    @Scheduled(cron = "0 30 3 * * *", zone = "UTC")
    @Transactional
    public void purgeExpiredTokens() {
        Instant now = clock.instant();
        int refresh = refreshTokenRepository.deleteExpired(now);
        int reset = resetTokenRepository.deleteExpiredOrUsed(now);
        if (refresh + reset > 0) {
            log.info("Purged {} expired refresh tokens and {} reset tokens", refresh, reset);
        }
    }
}
