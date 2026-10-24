package com.studenthub.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * A single injectable clock. Services ask this bean for "now" instead of calling
 * Instant.now() directly, so tests can pin time (e.g. to check overdue logic).
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
