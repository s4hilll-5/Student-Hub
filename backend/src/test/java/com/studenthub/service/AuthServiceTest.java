package com.studenthub.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.studenthub.dto.auth.ForgotPasswordRequest;
import com.studenthub.dto.auth.RegisterRequest;
import com.studenthub.dto.auth.ResetPasswordRequest;
import com.studenthub.entity.PasswordResetToken;
import com.studenthub.entity.RevokeReason;
import com.studenthub.entity.Role;
import com.studenthub.entity.User;
import com.studenthub.exception.BadRequestException;
import com.studenthub.exception.ConflictException;
import com.studenthub.repository.PasswordResetTokenRepository;
import com.studenthub.repository.UserRepository;
import com.studenthub.security.TokenHasher;
import com.studenthub.service.notification.PasswordResetNotifier;
import com.studenthub.support.TestData;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-16T10:00:00Z");

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private SessionTokenService sessionTokenService;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private PasswordResetTokenRepository resetTokenRepository;
    @Mock
    private PasswordResetNotifier resetNotifier;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, authenticationManager, sessionTokenService,
                refreshTokenService, resetTokenRepository, resetNotifier, TestData.properties(),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void registerRejectsADuplicateEmailIgnoringCase() {
        when(userRepository.existsByEmail("asha@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("Asha", "  Asha@Example.COM ", "secret123", null)))
                .isInstanceOf(ConflictException.class)
                .hasMessage(AuthService.DUPLICATE_EMAIL);
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void registerStoresANormalisedEmailAndOnlyTheBcryptHash() {
        when(userRepository.existsByEmail("asha@example.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("$2a$12$hashed");
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        authService.register(new RegisterRequest(" Asha Rao ", "Asha@Example.com", "secret123", "Asia/Kolkata"));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(saved.capture());
        User user = saved.getValue();
        assertThat(user.getEmail()).isEqualTo("asha@example.com");
        assertThat(user.getFullName()).isEqualTo("Asha Rao");
        assertThat(user.getPasswordHash()).isEqualTo("$2a$12$hashed").doesNotContain("secret123");
        assertThat(user.getRole()).isEqualTo(Role.STUDENT);
        assertThat(user.getTimezone()).isEqualTo("Asia/Kolkata");
        verify(sessionTokenService).issue(user);
    }

    @Test
    void forgotPasswordForUnknownEmailDoesNothingObservable() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        authService.requestPasswordReset(new ForgotPasswordRequest("nobody@example.com"));

        verifyNoInteractions(resetNotifier);
        verify(resetTokenRepository, never()).save(any());
    }

    @Test
    void forgotPasswordStoresOnlyTheHashOfTheEmailedToken() {
        User user = TestData.user(3L);
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        authService.requestPasswordReset(new ForgotPasswordRequest(user.getEmail()));

        ArgumentCaptor<String> link = ArgumentCaptor.forClass(String.class);
        verify(resetNotifier).sendPasswordReset(eq(user.getEmail()), anyString(), link.capture(), eq(Duration.ofMinutes(30)));
        String rawToken = link.getValue().substring(link.getValue().indexOf("token=") + 6);
        assertThat(link.getValue()).startsWith("http://localhost:5173/reset-password?token=");

        ArgumentCaptor<PasswordResetToken> stored = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(resetTokenRepository).save(stored.capture());
        assertThat(stored.getValue().getTokenHash())
                .isEqualTo(TokenHasher.sha256(rawToken))
                .isNotEqualTo(rawToken);
        assertThat(stored.getValue().getExpiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(30)));
        verify(resetTokenRepository).deleteAllForUser(3L);
    }

    @Test
    void expiredResetTokenIsRejectedAndThePasswordIsUnchanged() {
        User user = TestData.user(3L);
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setTokenHash(TokenHasher.sha256("raw"));
        token.setExpiresAt(NOW.minusSeconds(1));
        when(resetTokenRepository.findByTokenHash(TokenHasher.sha256("raw"))).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> authService.resetPassword(new ResetPasswordRequest("raw", "newpass123")))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(AuthService.INVALID_RESET_LINK);
        assertThat(user.getPasswordHash()).isEqualTo("$2a$04$hash");
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void validResetChangesPasswordInvalidatesTokensAndIsSingleUse() {
        User user = TestData.user(3L);
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setTokenHash(TokenHasher.sha256("raw"));
        token.setExpiresAt(NOW.plusSeconds(600));
        when(resetTokenRepository.findByTokenHash(TokenHasher.sha256("raw"))).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("newpass123")).thenReturn("$2a$12$new");

        authService.resetPassword(new ResetPasswordRequest("raw", "newpass123"));

        assertThat(user.getPasswordHash()).isEqualTo("$2a$12$new");
        assertThat(user.getTokenVersion()).isEqualTo(1);
        assertThat(token.getUsedAt()).isEqualTo(NOW);
        assertThat(token.isUsable(NOW)).isFalse();
        verify(refreshTokenService).revokeAll(3L, RevokeReason.PASSWORD_CHANGED);
    }
}
