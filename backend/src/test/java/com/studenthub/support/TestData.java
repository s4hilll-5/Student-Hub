package com.studenthub.support;

import java.time.Duration;
import java.util.List;

import com.studenthub.config.AppProperties;
import com.studenthub.entity.Role;
import com.studenthub.entity.User;

/** Builders shared by unit tests. */
public final class TestData {

    public static final String TEST_SECRET =
            "KP3hvwxOIY2TOkDjd2bPYO6m5rGZhNJihXDfZMLKlpHYikMEF2KtO31Poe7CDxzI+n5Ayv/oMXo3dKqjPTnUPw==";

    private TestData() {
    }

    public static AppProperties properties() {
        return properties(TEST_SECRET);
    }

    public static AppProperties properties(String secret) {
        return new AppProperties(
                "http://localhost:5173",
                new AppProperties.Jwt(secret, "studenthub", Duration.ofMinutes(15), Duration.ofDays(7), Duration.ofSeconds(20)),
                new AppProperties.Cookie(false, "Strict"),
                new AppProperties.Cors(List.of("http://localhost:5173")),
                new AppProperties.Security(4),
                new AppProperties.PasswordReset(Duration.ofMinutes(30)),
                new AppProperties.Mail(false, "StudentHub <no-reply@studenthub.local>"),
                new AppProperties.Admin(null, null),
                new AppProperties.RateLimit(true));
    }

    public static User user(long id) {
        User user = new User();
        user.setId(id);
        user.setFullName("Student " + id);
        user.setEmail("student" + id + "@example.com");
        user.setPasswordHash("$2a$04$hash");
        user.setRole(Role.STUDENT);
        user.setTimezone("Asia/Kolkata");
        return user;
    }
}
