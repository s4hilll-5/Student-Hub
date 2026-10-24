package com.studenthub.security;

import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Small in-memory fixed-window rate limiter keyed by e.g. "login:203.0.113.7".
 *
 * Good enough for a single backend instance. When running several instances, move the
 * counters to a shared store (Redis with Bucket4j, or the API gateway's rate limiting).
 */
public class FixedWindowRateLimiter {

    private static final int CLEANUP_THRESHOLD = 10_000;

    private final Map<String, Window> windows = new ConcurrentHashMap<>();
    private final Clock clock;

    public FixedWindowRateLimiter(Clock clock) {
        this.clock = clock;
    }

    /**
     * Records one attempt.
     *
     * @return 0 if allowed, otherwise the number of seconds until the window resets
     */
    public long tryAcquire(String key, int maxAttempts, Duration window) {
        long now = clock.millis();
        long windowMillis = window.toMillis();
        if (windows.size() > CLEANUP_THRESHOLD) {
            windows.entrySet().removeIf(e -> now - e.getValue().start >= windowMillis);
        }
        Window current = windows.compute(key, (k, existing) -> {
            if (existing == null || now - existing.start >= windowMillis) {
                return new Window(now, 1);
            }
            return new Window(existing.start, existing.count + 1);
        });
        if (current.count <= maxAttempts) {
            return 0;
        }
        long remainingMillis = windowMillis - (now - current.start);
        return Math.max(1, (remainingMillis + 999) / 1000);
    }

    public void reset() {
        windows.clear();
    }

    private record Window(long start, int count) {
    }
}
