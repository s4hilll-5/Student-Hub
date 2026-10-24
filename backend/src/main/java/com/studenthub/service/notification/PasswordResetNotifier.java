package com.studenthub.service.notification;

import java.time.Duration;

/** Delivers password reset links. Implementations must never throw back into the request. */
public interface PasswordResetNotifier {

    void sendPasswordReset(String email, String fullName, String resetLink, Duration validFor);
}
