package com.studenthub.service.notification;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.studenthub.config.AppProperties;

/**
 * Sends the reset email over SMTP (MAIL_ENABLED=true). Runs asynchronously so the
 * forgot-password endpoint answers in roughly the same time whether or not the email
 * exists, which helps against account enumeration by timing.
 */
@Component
@ConditionalOnProperty(name = "app.mail.enabled", havingValue = "true")
public class MailPasswordResetNotifier implements PasswordResetNotifier {

    private static final Logger log = LoggerFactory.getLogger(MailPasswordResetNotifier.class);

    private final JavaMailSender mailSender;
    private final AppProperties properties;

    public MailPasswordResetNotifier(JavaMailSender mailSender, AppProperties properties) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    @Async
    @Override
    public void sendPasswordReset(String email, String fullName, String resetLink, Duration validFor) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(properties.mail().from());
        message.setTo(email);
        message.setSubject("Reset your StudentHub password");
        message.setText("""
                Hi %s,

                Someone asked to reset the password for your StudentHub account.
                Open this link to choose a new password (it works once and expires in %d minutes):

                %s

                If you didn't ask for this, you can ignore this email. Your password stays the same.

                StudentHub
                """.formatted(fullName, validFor.toMinutes(), resetLink));
        try {
            mailSender.send(message);
        } catch (MailException e) {
            // Never surface delivery problems to the client: that would reveal the account exists.
            log.error("Could not send password reset email: {}", e.getMessage());
        }
    }
}
