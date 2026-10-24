package com.studenthub.service;

import java.time.Clock;
import java.time.Duration;
import java.util.Locale;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.studenthub.config.AppProperties;
import com.studenthub.dto.auth.ForgotPasswordRequest;
import com.studenthub.dto.auth.LoginRequest;
import com.studenthub.dto.auth.RegisterRequest;
import com.studenthub.dto.auth.ResetPasswordRequest;
import com.studenthub.entity.PasswordResetToken;
import com.studenthub.entity.RevokeReason;
import com.studenthub.entity.Role;
import com.studenthub.entity.User;
import com.studenthub.exception.BadRequestException;
import com.studenthub.exception.ConflictException;
import com.studenthub.exception.UnauthorizedException;
import com.studenthub.repository.PasswordResetTokenRepository;
import com.studenthub.repository.UserRepository;
import com.studenthub.security.AppUserPrincipal;
import com.studenthub.security.TokenHasher;
import com.studenthub.service.SessionTokenService.IssuedSession;
import com.studenthub.service.notification.PasswordResetNotifier;

@Service
public class AuthService {

    static final String DUPLICATE_EMAIL = "An account with this email already exists.";
    static final String INVALID_RESET_LINK = "This reset link is invalid or has expired. Request a new one.";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final SessionTokenService sessionTokenService;
    private final RefreshTokenService refreshTokenService;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final PasswordResetNotifier resetNotifier;
    private final AppProperties properties;
    private final Clock clock;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager, SessionTokenService sessionTokenService,
            RefreshTokenService refreshTokenService, PasswordResetTokenRepository resetTokenRepository,
            PasswordResetNotifier resetNotifier, AppProperties properties, Clock clock) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.sessionTokenService = sessionTokenService;
        this.refreshTokenService = refreshTokenService;
        this.resetTokenRepository = resetTokenRepository;
        this.resetNotifier = resetNotifier;
        this.properties = properties;
        this.clock = clock;
    }

    public static String normaliseEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    @Transactional
    public IssuedSession register(RegisterRequest request) {
        String email = normaliseEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException(DUPLICATE_EMAIL, "email");
        }
        User user = new User();
        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(Role.STUDENT);
        user.setTimezone(request.timezone() != null ? request.timezone() : "UTC");
        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            // Two registrations with the same email raced past the exists check.
            throw new ConflictException(DUPLICATE_EMAIL, "email");
        }
        return sessionTokenService.issue(user);
    }

    /**
     * Delegates the password check to Spring Security's AuthenticationManager. Wrong
     * password, unknown email and disabled account all surface as the same 401.
     */
    @Transactional
    public IssuedSession login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(normaliseEmail(request.email()), request.password()));
        AppUserPrincipal principal = (AppUserPrincipal) authentication.getPrincipal();
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password."));
        return sessionTokenService.issue(user);
    }

    @Transactional(noRollbackFor = UnauthorizedException.class)
    public IssuedSession refresh(String rawRefreshToken) {
        RefreshTokenService.Rotation rotation = refreshTokenService.rotate(rawRefreshToken);
        return sessionTokenService.withRefreshToken(rotation.user(), rotation.newRawToken());
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenService.revoke(rawRefreshToken, RevokeReason.LOGOUT);
    }

    /**
     * Always completes silently, whether or not the email belongs to an account, so the
     * response cannot be used to discover registered emails.
     */
    @Transactional
    public void requestPasswordReset(ForgotPasswordRequest request) {
        String email = normaliseEmail(request.email());
        Duration ttl = properties.passwordReset().tokenTtl();
        userRepository.findByEmail(email).filter(User::isEnabled).ifPresent(user -> {
            resetTokenRepository.deleteAllForUser(user.getId());
            String rawToken = TokenHasher.newToken();
            PasswordResetToken token = new PasswordResetToken();
            token.setUser(user);
            token.setTokenHash(TokenHasher.sha256(rawToken));
            token.setExpiresAt(clock.instant().plus(ttl));
            resetTokenRepository.save(token);
            String link = stripTrailingSlash(properties.frontendUrl()) + "/reset-password?token=" + rawToken;
            resetNotifier.sendPasswordReset(user.getEmail(), user.getFullName(), link, ttl);
        });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken token = resetTokenRepository.findByTokenHash(TokenHasher.sha256(request.token()))
                .filter(t -> t.isUsable(clock.instant()))
                .orElseThrow(() -> new BadRequestException(INVALID_RESET_LINK, "token"));
        User user = token.getUser();
        if (!user.isEnabled()) {
            throw new BadRequestException(INVALID_RESET_LINK, "token");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        // Invalidates every access token issued before the reset.
        user.incrementTokenVersion();
        token.setUsedAt(clock.instant());
        // And every refresh token: all devices must sign in again with the new password.
        refreshTokenService.revokeAll(user.getId(), RevokeReason.PASSWORD_CHANGED);
    }

    private static String stripTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
