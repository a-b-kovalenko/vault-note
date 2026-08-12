package com.andrii.vaultnote.app.service.impl;

import com.andrii.vaultnote.app.config.EmailVerificationProperties;
import com.andrii.vaultnote.app.exception.EmailVerificationFailedException;
import com.andrii.vaultnote.app.mail.MailMessage;
import com.andrii.vaultnote.app.mail.MailSender;
import com.andrii.vaultnote.app.security.SecureTokenGenerator;
import com.andrii.vaultnote.app.service.EmailVerificationService;
import com.andrii.vaultnote.users.infrastructure.persistence.entity.EmailVerificationTokenEntity;
import com.andrii.vaultnote.users.infrastructure.persistence.entity.UserEntity;
import com.andrii.vaultnote.users.infrastructure.persistence.repository.EmailVerificationTokenJpaRepository;
import com.andrii.vaultnote.users.infrastructure.persistence.repository.UserJpaRepository;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import static java.util.Objects.isNull;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EmailVerificationServiceImpl implements EmailVerificationService {

  private static final String VERIFICATION_PATH = "/verify-email";
  private static final String EMAIL_SUBJECT = "Verify your VaultNote email";
  private static final String EMAIL_BODY = "Hello %s,\n\n"
      + "Please verify your VaultNote email by opening this link:\n%s";

  UserJpaRepository userJpaRepository;
  EmailVerificationTokenJpaRepository tokenRepository;
  SecureTokenGenerator tokenGenerator;
  EmailVerificationProperties properties;
  MailSender mailSender;
  Clock clock;

  @Override
  @Transactional
  public void issueVerificationEmail(UserEntity user) {
    log.info("Issuing email verification for userId={}", user.getId());
    var generatedToken = tokenGenerator.generate();
    var token = EmailVerificationTokenEntity.builder()
        .user(user)
        .tokenHash(generatedToken.hash())
        .expiresAt(clock.instant().plus(properties.tokenTtl()))
        .build();
    tokenRepository.save(token);

    var verificationUrl = UriComponentsBuilder
        .fromUriString(properties.baseUrl())
        .path(VERIFICATION_PATH)
        .queryParam("token", generatedToken.rawValue())
        .build()
        .toUriString();
    var message = new MailMessage(
        user.getEmail(),
        EMAIL_SUBJECT,
        EMAIL_BODY.formatted(user.getDisplayName(), verificationUrl));

    mailSender.send(message);
  }

  @Override
  @Transactional
  public void verifyEmail(String rawToken) {
    var tokenHash = Optional.ofNullable(rawToken)
        .map(tokenGenerator::hash)
        .orElseThrow(EmailVerificationFailedException::new);
    var now = clock.instant();
    var verifiedUser = tokenRepository.findByTokenHash(tokenHash)
        .filter(t -> isNull(t.getUsedAt()) && t.getExpiresAt().isAfter(now))
        .map(token -> markTokenUsed(token, now))
        .map(EmailVerificationTokenEntity::getUser)
        .map(this::markEmailVerified)
        .orElseThrow(EmailVerificationFailedException::new);
    log.info("Email verified for userId={}", verifiedUser.getId());
  }

  private EmailVerificationTokenEntity markTokenUsed(
      EmailVerificationTokenEntity token, Instant usedAt) {
    var usedToken = token.toBuilder()
        .usedAt(usedAt)
        .build();
    return tokenRepository.save(usedToken);
  }

  private UserEntity markEmailVerified(UserEntity user) {
    var verifiedUser = user.toBuilder()
        .emailVerified(true)
        .build();
    return userJpaRepository.save(verifiedUser);
  }
}
