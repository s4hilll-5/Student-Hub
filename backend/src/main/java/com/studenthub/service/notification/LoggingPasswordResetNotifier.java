package com.studenthub.service.notification;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Development fallback used when MAIL_ENABLED=false: prints the reset link to the
 * backend console so the flow can be tested without an SMTP server.
 *
 * Do not run production with this notifier. Anyone who can read the logs could reset
 * passwords. Set MAIL_ENABLED=true and configure SMTP instead.
 */
@Component
@ConditionalOnProperty(name = "app.mail.enabled", havingValue = "false", matchIfMissing = true)
public class LoggingPasswordResetNotifier implements PasswordResetNotifier {

    private static final Logger log = LoggerFactory.getLogger(LoggingPasswordResetNotifier.class);

    @Override
    public void sendPasswordReset(String email, String fullName, String resetLink, Duration validFor) {
        log.warn("[DEV ONLY - mail disabled] Password reset link for {} (valid {} min): {}",
                email, validFor.toMinutes(), resetLink);
    }
}
