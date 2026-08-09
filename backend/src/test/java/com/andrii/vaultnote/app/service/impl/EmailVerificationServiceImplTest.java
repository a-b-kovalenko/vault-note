package com.andrii.vaultnote.app.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.andrii.vaultnote.app.config.EmailVerificationProperties;
import com.andrii.vaultnote.app.mail.MailMessage;
import com.andrii.vaultnote.app.mail.MailSender;
import com.andrii.vaultnote.app.security.EmailVerificationTokenGenerator;
import com.andrii.vaultnote.users.infrastructure.persistence.entity.EmailVerificationTokenEntity;
import com.andrii.vaultnote.users.infrastructure.persistence.entity.UserEntity;
import com.andrii.vaultnote.users.infrastructure.persistence.repository.EmailVerificationTokenJpaRepository;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.web.util.UriComponentsBuilder;

class EmailVerificationServiceImplTest {

  private static final String EMAIL = "user@example.com";
  private static final String RAW_TOKEN = "raw-token";
  private static final String TOKEN_HASH = "token-hash";
  private static final String BASE_URL = "http://localhost:4200";
  private static final Duration TOKEN_TTL = Duration.ofHours(24);

  private final EmailVerificationTokenJpaRepository tokenRepository = Mockito
      .mock(EmailVerificationTokenJpaRepository.class);
  private final EmailVerificationTokenGenerator tokenGenerator = Mockito.mock(EmailVerificationTokenGenerator.class);
  private final MailSender mailSender = Mockito.mock(MailSender.class);
  private final EmailVerificationServiceImpl emailVerificationService = new EmailVerificationServiceImpl(
      tokenRepository,
      tokenGenerator,
      new EmailVerificationProperties(BASE_URL, TOKEN_TTL),
      mailSender);

  @Test
  void shouldPersistTokenAndSendVerificationEmail() {
    var user = UserEntity.builder()
        .id(1L)
        .email(EMAIL)
        .build();
    var generatedToken = new EmailVerificationTokenGenerator.GeneratedToken(RAW_TOKEN, TOKEN_HASH);
    when(tokenGenerator.generate()).thenReturn(generatedToken);
    var startedAt = Instant.now();

    emailVerificationService.issueVerificationEmail(user);

    var finishedAt = Instant.now();
    var tokenCaptor = ArgumentCaptor.forClass(EmailVerificationTokenEntity.class);
    verify(tokenRepository).save(tokenCaptor.capture());
    var savedToken = tokenCaptor.getValue();
    assertThat(savedToken.getUser()).isSameAs(user);
    assertThat(savedToken.getTokenHash()).isEqualTo(TOKEN_HASH);
    assertThat(savedToken.getExpiresAt())
        .isBetween(startedAt.plus(TOKEN_TTL), finishedAt.plus(TOKEN_TTL));

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
}
