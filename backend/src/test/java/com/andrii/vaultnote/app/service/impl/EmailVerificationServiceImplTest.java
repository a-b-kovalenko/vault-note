package com.andrii.vaultnote.app.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.andrii.vaultnote.app.config.EmailVerificationProperties;
import com.andrii.vaultnote.app.exception.EmailVerificationFailedException;
import com.andrii.vaultnote.app.mail.MailMessage;
import com.andrii.vaultnote.app.mail.MailSender;
import com.andrii.vaultnote.app.security.EmailVerificationTokenGenerator;
import com.andrii.vaultnote.users.infrastructure.persistence.entity.EmailVerificationTokenEntity;
import com.andrii.vaultnote.users.infrastructure.persistence.entity.UserEntity;
import com.andrii.vaultnote.users.infrastructure.persistence.repository.EmailVerificationTokenJpaRepository;
import com.andrii.vaultnote.users.infrastructure.persistence.repository.UserJpaRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.util.UriComponentsBuilder;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceImplTest {

  private static final String EMAIL = "user@example.com";
  private static final String RAW_TOKEN = "raw-token";
  private static final String TOKEN_HASH = "token-hash";
  private static final String BASE_URL = "http://localhost:4200";
  private static final Duration TOKEN_TTL = Duration.ofHours(24);
  private static final Instant NOW = Instant.parse("2026-08-09T12:00:00Z");

  @Mock
  UserJpaRepository userJpaRepository;

  @Mock
  EmailVerificationTokenJpaRepository tokenRepository;

  @Mock
  EmailVerificationTokenGenerator tokenGenerator;

  @Mock
  EmailVerificationProperties properties;

  @Mock
  MailSender mailSender;

  @Mock
  Clock clock;

  @InjectMocks
  EmailVerificationServiceImpl emailVerificationService;

  @Test
  void shouldPersistTokenAndSendVerificationEmail() {
    var user = UserEntity.builder()
        .id(1L)
        .email(EMAIL)
        .build();
    var generatedToken = new EmailVerificationTokenGenerator.GeneratedToken(RAW_TOKEN, TOKEN_HASH);
    when(tokenGenerator.generate()).thenReturn(generatedToken);
    when(properties.baseUrl()).thenReturn(BASE_URL);
    when(properties.tokenTtl()).thenReturn(TOKEN_TTL);
    when(clock.instant()).thenReturn(NOW);

    emailVerificationService.issueVerificationEmail(user);

    var tokenCaptor = ArgumentCaptor.forClass(EmailVerificationTokenEntity.class);
    verify(tokenRepository).save(tokenCaptor.capture());
    var savedToken = tokenCaptor.getValue();
    assertThat(savedToken.getUser()).isSameAs(user);
    assertThat(savedToken.getTokenHash()).isEqualTo(TOKEN_HASH);
    assertThat(savedToken.getExpiresAt()).isEqualTo(NOW.plus(TOKEN_TTL));

    var messageCaptor = ArgumentCaptor.forClass(MailMessage.class);
    verify(mailSender).send(messageCaptor.capture());
    var sentMessage = messageCaptor.getValue();
    var expectedUrl = UriComponentsBuilder
        .fromUriString(BASE_URL)
        .path("/verify-email")
        .queryParam("token", RAW_TOKEN)
        .build()
        .toUriString();
    assertThat(sentMessage.to()).isEqualTo(EMAIL);
    assertThat(sentMessage.subject()).isEqualTo("Verify your VaultNote email");
    assertThat(sentMessage.text()).contains(expectedUrl);
  }

  @Test
  void shouldVerifyEmailAndMarkTokenAndUser() {
    var user = UserEntity.builder()
        .id(1L)
        .email(EMAIL)
        .emailVerified(false)
        .build();
    var token = EmailVerificationTokenEntity.builder()
        .user(user)
        .tokenHash(TOKEN_HASH)
        .expiresAt(NOW.plus(TOKEN_TTL))
        .build();
    when(tokenGenerator.hash(RAW_TOKEN)).thenReturn(TOKEN_HASH);
    when(clock.instant()).thenReturn(NOW);
    when(tokenRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(token));
    when(tokenRepository.save(any(EmailVerificationTokenEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(userJpaRepository.save(any(UserEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    emailVerificationService.verifyEmail(RAW_TOKEN);

    var tokenCaptor = ArgumentCaptor.forClass(EmailVerificationTokenEntity.class);
    verify(tokenRepository).save(tokenCaptor.capture());
    assertThat(tokenCaptor.getValue().getUsedAt()).isEqualTo(NOW);

    var userCaptor = ArgumentCaptor.forClass(UserEntity.class);
    verify(userJpaRepository).save(userCaptor.capture());
    assertThat(userCaptor.getValue().isEmailVerified()).isTrue();
  }

  @Test
  void shouldRejectNullToken() {
    assertThatThrownBy(() -> emailVerificationService.verifyEmail(null))
        .isInstanceOf(EmailVerificationFailedException.class)
        .hasMessage("Email verification link is invalid or has expired.");

    verifyNoInteractions(tokenGenerator, tokenRepository, userJpaRepository, clock);
  }

  @Test
  void shouldRejectUnknownToken() {
    when(tokenGenerator.hash(RAW_TOKEN)).thenReturn(TOKEN_HASH);
    when(tokenRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> emailVerificationService.verifyEmail(RAW_TOKEN))
        .isInstanceOf(EmailVerificationFailedException.class);

    verifyNoInteractions(userJpaRepository);
  }

  @Test
  void shouldRejectExpiredToken() {
    var token = EmailVerificationTokenEntity.builder()
        .user(UserEntity.builder().email(EMAIL).build())
        .tokenHash(TOKEN_HASH)
        .expiresAt(NOW.minusSeconds(1))
        .build();
    when(tokenGenerator.hash(RAW_TOKEN)).thenReturn(TOKEN_HASH);
    when(clock.instant()).thenReturn(NOW);
    when(tokenRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(token));

    assertThatThrownBy(() -> emailVerificationService.verifyEmail(RAW_TOKEN))
        .isInstanceOf(EmailVerificationFailedException.class);

    verify(tokenRepository).findByTokenHash(TOKEN_HASH);
    verifyNoInteractions(userJpaRepository);
  }

  @Test
  void shouldRejectUsedToken() {
    var token = EmailVerificationTokenEntity.builder()
        .user(UserEntity.builder().email(EMAIL).build())
        .tokenHash(TOKEN_HASH)
        .expiresAt(NOW.plus(TOKEN_TTL))
        .usedAt(NOW.minusSeconds(1))
        .build();
    when(tokenGenerator.hash(RAW_TOKEN)).thenReturn(TOKEN_HASH);
    when(clock.instant()).thenReturn(NOW);
    when(tokenRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(token));

    assertThatThrownBy(() -> emailVerificationService.verifyEmail(RAW_TOKEN))
        .isInstanceOf(EmailVerificationFailedException.class);

    verify(tokenRepository).findByTokenHash(TOKEN_HASH);
    verifyNoInteractions(userJpaRepository);
  }
}
