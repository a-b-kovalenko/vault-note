package com.andrii.vaultnote.app.service.impl;

import com.andrii.vaultnote.app.api.auth.dto.PasswordResetConfirmRequest;
import com.andrii.vaultnote.app.api.auth.dto.PasswordResetRequest;
import com.andrii.vaultnote.app.config.PasswordResetProperties;
import com.andrii.vaultnote.app.exception.PasswordResetFailedException;
import com.andrii.vaultnote.app.mail.MailMessage;
import com.andrii.vaultnote.app.mail.MailSender;
import com.andrii.vaultnote.app.security.SecureTokenGenerator;
import com.andrii.vaultnote.app.security.ratelimit.RateLimitScope;
import com.andrii.vaultnote.app.service.PasswordResetService;
import com.andrii.vaultnote.app.service.RateLimitService;
import com.andrii.vaultnote.users.infrastructure.persistence.entity.PasswordResetTokenEntity;
import com.andrii.vaultnote.users.infrastructure.persistence.entity.UserEntity;
import com.andrii.vaultnote.users.infrastructure.persistence.repository.EmailVerificationTokenJpaRepository;
import com.andrii.vaultnote.users.infrastructure.persistence.repository.PasswordResetTokenJpaRepository;
import com.andrii.vaultnote.users.infrastructure.persistence.repository.RefreshTokenJpaRepository;
import com.andrii.vaultnote.users.infrastructure.persistence.repository.UserJpaRepository;
import java.time.Clock;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import static java.util.Objects.isNull;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PasswordResetServiceImpl implements PasswordResetService {

  private static final String RESET_PATH = "/reset-password";
  private static final String EMAIL_SUBJECT = "Reset your VaultNote password";
  private static final String EMAIL_BODY = "Hello %s,\n\n"
    + "Reset your VaultNote password and verify your email by opening this link:\n%s\n\n"
    + "If you did not request this, you can ignore this email.";

  UserJpaRepository userRepository;
  PasswordResetTokenJpaRepository passwordResetTokenRepository;
  EmailVerificationTokenJpaRepository emailVerificationTokenRepository;
  RefreshTokenJpaRepository refreshTokenRepository;
  SecureTokenGenerator tokenGenerator;
  PasswordResetProperties properties;
  MailSender mailSender;
  PasswordEncoder passwordEncoder;
  Clock clock;
  RateLimitService rateLimitService;

  @Override
  @Transactional
  public void requestPasswordReset(PasswordResetRequest request, String clientIp) {
    rateLimitService.check(RateLimitScope.PASSWORD_RESET, clientIp, request.email());
    log.info("Received password reset request for email={}", maskEmail(request.email()));
    userRepository.findByEmail(request.email())
      .ifPresent(this::issuePasswordReset);
  }

  @Override
  @Transactional
  public void confirmPasswordReset(PasswordResetConfirmRequest request) {
    var tokenHash = Optional.ofNullable(request.token())
      .map(tokenGenerator::hash)
      .orElseThrow(PasswordResetFailedException::new);
    var now = clock.instant();
    var token = passwordResetTokenRepository.findByTokenHash(tokenHash)
      .filter(resetToken -> isNull(resetToken.getUsedAt()))
      .filter(resetToken -> isNull(resetToken.getInvalidatedAt()))
      .filter(resetToken -> resetToken.getExpiresAt().isAfter(now))
      .orElseThrow(PasswordResetFailedException::new);

    var user = token.getUser();
    var updatedUser = user.toBuilder()
      .passwordHash(passwordEncoder.encode(request.newPassword()))
      .emailVerified(true)
      .build();
    userRepository.save(updatedUser);

    passwordResetTokenRepository.save(token.toBuilder()
      .usedAt(now)
      .build());
    emailVerificationTokenRepository.invalidateActiveByUserId(user.getId(), now);
    var revokedRefreshTokens = refreshTokenRepository.revokeActiveByUserId(user.getId(), now);

    log.info(
      "Password reset completed for userId={}; revokedRefreshTokens={}",
      user.getId(),
      revokedRefreshTokens);
  }

  private void issuePasswordReset(UserEntity user) {
    var now = clock.instant();
    var invalidatedTokens = passwordResetTokenRepository
      .invalidateActiveByUserId(user.getId(), now);
    var generatedToken = tokenGenerator.generate();
    var token = PasswordResetTokenEntity.builder()
      .user(user)
      .tokenHash(generatedToken.hash())
      .expiresAt(now.plus(properties.tokenTtl()))
      .build();
    passwordResetTokenRepository.save(token);

    var resetUrl = UriComponentsBuilder
      .fromUriString(properties.baseUrl())
      .path(RESET_PATH)
      .queryParam("token", generatedToken.rawValue())
      .build()
      .toUriString();
    var message = new MailMessage(
      user.getEmail(),
      EMAIL_SUBJECT,
      EMAIL_BODY.formatted(user.getDisplayName(), resetUrl));

    mailSender.send(message);
    log.info(
      "Password reset email issued for userId={}; invalidatedTokens={}",
      user.getId(),
      invalidatedTokens);
  }

  private static String maskEmail(String email) {
    if (isNull(email)) {
      return "***";
    }

    var atIndex = email.indexOf('@');
    if (atIndex <= 0 || atIndex == email.length() - 1) {
      return "***";
    }

    var localPart = email.substring(0, atIndex);
    var domain = email.substring(atIndex + 1);
    var visibleLocalLength = Math.min(2, localPart.length());
    var maskedLocalPart = localPart.substring(0, visibleLocalLength)
      + "*".repeat(Math.max(1, localPart.length() - visibleLocalLength));
    var topLevelDomainIndex = domain.lastIndexOf('.');
    var maskedDomain = topLevelDomainIndex > 0
      ? "***" + domain.substring(topLevelDomainIndex)
      : "***";

    return maskedLocalPart + "@" + maskedDomain;
  }
}
