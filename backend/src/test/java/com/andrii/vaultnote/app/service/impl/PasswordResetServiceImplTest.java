package com.andrii.vaultnote.app.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.andrii.vaultnote.app.api.auth.dto.PasswordResetConfirmRequest;
import com.andrii.vaultnote.app.api.auth.dto.PasswordResetRequest;
import com.andrii.vaultnote.app.config.PasswordResetProperties;
import com.andrii.vaultnote.app.exception.PasswordResetFailedException;
import com.andrii.vaultnote.app.mail.MailMessage;
import com.andrii.vaultnote.app.mail.MailSender;
import com.andrii.vaultnote.app.security.SecureTokenGenerator;
import com.andrii.vaultnote.app.security.SecureTokenGenerator.GeneratedToken;
import com.andrii.vaultnote.users.domain.UserRole;
import com.andrii.vaultnote.users.infrastructure.persistence.entity.PasswordResetTokenEntity;
import com.andrii.vaultnote.users.infrastructure.persistence.entity.UserEntity;
import com.andrii.vaultnote.users.infrastructure.persistence.repository.EmailVerificationTokenJpaRepository;
import com.andrii.vaultnote.users.infrastructure.persistence.repository.PasswordResetTokenJpaRepository;
import com.andrii.vaultnote.users.infrastructure.persistence.repository.RefreshTokenJpaRepository;
import com.andrii.vaultnote.users.infrastructure.persistence.repository.UserJpaRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
@FieldDefaults(level = AccessLevel.PRIVATE)
class PasswordResetServiceImplTest {

  private static final String EMAIL = "user@example.com";
  private static final String DISPLAY_NAME = "Vault User";
  private static final String RAW_TOKEN = "raw-reset-token";
  private static final String TOKEN_HASH = "hashed-reset-token";
  private static final String OLD_PASSWORD_HASH = "old-password-hash";
  private static final String NEW_PASSWORD_HASH = "new-password-hash";
  private static final Instant NOW = Instant.parse("2099-01-01T00:00:00Z");

  @Mock
  UserJpaRepository userRepository;
  @Mock
  PasswordResetTokenJpaRepository passwordResetTokenRepository;
  @Mock
  EmailVerificationTokenJpaRepository emailVerificationTokenRepository;
  @Mock
  RefreshTokenJpaRepository refreshTokenRepository;
  @Mock
  SecureTokenGenerator tokenGenerator;
  @Mock
  PasswordResetProperties properties;
  @Mock
  MailSender mailSender;
  @Mock
  PasswordEncoder passwordEncoder;
  @Mock
  Clock clock;

  @InjectMocks
  PasswordResetServiceImpl passwordResetService;

  @Test
  void shouldIssuePasswordResetEmailForExistingUser() {
    var user = user(false);
    var generatedToken = new GeneratedToken(RAW_TOKEN, TOKEN_HASH);
    var request = PasswordResetRequest.builder().email(EMAIL).build();

    when(clock.instant()).thenReturn(NOW);
    when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
    when(properties.baseUrl()).thenReturn("http://localhost:4200");
    when(properties.tokenTtl()).thenReturn(Duration.ofHours(1));
    when(tokenGenerator.generate()).thenReturn(generatedToken);
    when(passwordResetTokenRepository.invalidateActiveByUserId(user.getId(), NOW)).thenReturn(1);

    passwordResetService.requestPasswordReset(request);

    verify(passwordResetTokenRepository).invalidateActiveByUserId(user.getId(), NOW);
    var tokenCaptor = ArgumentCaptor.forClass(PasswordResetTokenEntity.class);
    verify(passwordResetTokenRepository).save(tokenCaptor.capture());
    assertThat(tokenCaptor.getValue())
      .extracting(PasswordResetTokenEntity::getUser, PasswordResetTokenEntity::getTokenHash,
        PasswordResetTokenEntity::getExpiresAt)
      .containsExactly(user, TOKEN_HASH, NOW.plus(Duration.ofHours(1)));
    var messageCaptor = ArgumentCaptor.forClass(MailMessage.class);
    verify(mailSender).send(messageCaptor.capture());
    assertThat(messageCaptor.getValue())
      .extracting(MailMessage::to, MailMessage::subject, MailMessage::text)
      .containsExactly(
        EMAIL,
        "Reset your VaultNote password",
        "Hello Vault User,\n\n"
          + "Reset your VaultNote password and verify your email by opening this link:\n"
          + "http://localhost:4200/reset-password?token=raw-reset-token\n\n"
          + "If you did not request this, you can ignore this email.");
  }

  @Test
  void shouldNotRevealUnknownEmail() {
    var request = PasswordResetRequest.builder().email(EMAIL).build();
    when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

    passwordResetService.requestPasswordReset(request);

    verifyNoInteractions(
      passwordResetTokenRepository,
      emailVerificationTokenRepository,
      refreshTokenRepository,
      tokenGenerator,
      mailSender,
      passwordEncoder);
  }

  @Test
  void shouldConfirmPasswordResetAndRevokeSessions() {
    var user = user(false);
    var token = PasswordResetTokenEntity.builder()
      .id(17L)
      .user(user)
      .tokenHash(TOKEN_HASH)
      .expiresAt(NOW.plus(Duration.ofHours(1)))
      .build();
    var request = PasswordResetConfirmRequest.builder()
      .token(RAW_TOKEN)
      .newPassword("newPassword1234")
      .build();

    when(clock.instant()).thenReturn(NOW);
    when(tokenGenerator.hash(RAW_TOKEN)).thenReturn(TOKEN_HASH);
    when(passwordResetTokenRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(token));
    when(passwordEncoder.encode("newPassword1234")).thenReturn(NEW_PASSWORD_HASH);
    when(refreshTokenRepository.revokeActiveByUserId(user.getId(), NOW)).thenReturn(2);

    passwordResetService.confirmPasswordReset(request);

    var userCaptor = ArgumentCaptor.forClass(UserEntity.class);
    verify(userRepository).save(userCaptor.capture());
    assertThat(userCaptor.getValue())
      .extracting(UserEntity::getPasswordHash, UserEntity::isEmailVerified)
      .containsExactly(NEW_PASSWORD_HASH, true);
    var tokenCaptor = ArgumentCaptor.forClass(PasswordResetTokenEntity.class);
    verify(passwordResetTokenRepository).save(tokenCaptor.capture());
    assertThat(tokenCaptor.getValue().getUsedAt()).isEqualTo(NOW);
    verify(emailVerificationTokenRepository)
      .invalidateActiveByUserId(user.getId(), NOW);
    verify(refreshTokenRepository).revokeActiveByUserId(user.getId(), NOW);
  }

  @Test
  void shouldRejectExpiredPasswordResetToken() {
    var user = user(false);
    var token = PasswordResetTokenEntity.builder()
      .user(user)
      .tokenHash(TOKEN_HASH)
      .expiresAt(NOW.minusSeconds(1))
      .build();
    var request = PasswordResetConfirmRequest.builder()
      .token(RAW_TOKEN)
      .newPassword("newPassword1234")
      .build();
    when(clock.instant()).thenReturn(NOW);
    when(tokenGenerator.hash(RAW_TOKEN)).thenReturn(TOKEN_HASH);
    when(passwordResetTokenRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(token));

    assertThatExceptionOfType(PasswordResetFailedException.class)
      .isThrownBy(() -> passwordResetService.confirmPasswordReset(request))
      .withMessage("Password reset link is invalid or has expired.");

    verify(userRepository, never()).save(any(UserEntity.class));
    verify(passwordResetTokenRepository, never()).save(any(PasswordResetTokenEntity.class));
    verifyNoInteractions(emailVerificationTokenRepository, refreshTokenRepository, passwordEncoder);
  }

  private static UserEntity user(boolean emailVerified) {
    return UserEntity.builder()
      .id(42L)
      .email(EMAIL)
      .displayName(DISPLAY_NAME)
      .passwordHash(OLD_PASSWORD_HASH)
      .emailVerified(emailVerified)
      .roles(EnumSet.of(UserRole.USER))
      .build();
  }
}
