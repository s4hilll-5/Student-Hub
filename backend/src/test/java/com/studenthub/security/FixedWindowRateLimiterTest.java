package com.studenthub.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

class FixedWindowRateLimiterTest {

    /** A clock the test can move forward. */
    static final class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant start) {
            this.now = start;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    @Test
    void blocksAfterLimitAndResetsWithTheWindow() {
        MutableClock clock = new MutableClock(Instant.parse("2026-10-16T10:00:00Z"));
        FixedWindowRateLimiter limiter = new FixedWindowRateLimiter(clock);

        for (int i = 0; i < 3; i++) {
            assertThat(limiter.tryAcquire("login:1.2.3.4", 3, Duration.ofMinutes(1))).isZero();
        }
        long retryAfter = limiter.tryAcquire("login:1.2.3.4", 3, Duration.ofMinutes(1));
        assertThat(retryAfter).isBetween(1L, 60L);

        // Other clients are unaffected.
        assertThat(limiter.tryAcquire("login:5.6.7.8", 3, Duration.ofMinutes(1))).isZero();

        clock.advance(Duration.ofSeconds(61));
        assertThat(limiter.tryAcquire("login:1.2.3.4", 3, Duration.ofMinutes(1))).isZero();
    }
}
