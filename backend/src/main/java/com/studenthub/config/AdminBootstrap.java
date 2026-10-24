package com.studenthub.config;

import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.studenthub.entity.Role;
import com.studenthub.entity.User;
import com.studenthub.repository.UserRepository;

/**
 * Creates the first ADMIN account from ADMIN_EMAIL and ADMIN_PASSWORD, if both are set
 * and no account uses that email yet. Public registration only ever creates STUDENTs.
 */
@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);
    private static final int MIN_ADMIN_PASSWORD_LENGTH = 12;

    private final AppProperties properties;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminBootstrap(AppProperties properties, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.properties = properties;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        AppProperties.Admin admin = properties.admin();
        if (admin == null || isBlank(admin.email()) || isBlank(admin.password())) {
            return;
        }
        String email = admin.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmail(email)) {
            return;
        }
        if (admin.password().length() < MIN_ADMIN_PASSWORD_LENGTH) {
            log.warn("ADMIN_PASSWORD must be at least {} characters; admin account not created", MIN_ADMIN_PASSWORD_LENGTH);
            return;
        }
        User user = new User();
        user.setFullName("Administrator");
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(admin.password()));
        user.setRole(Role.ADMIN);
        userRepository.save(user);
        log.info("Created admin account {}", email);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
