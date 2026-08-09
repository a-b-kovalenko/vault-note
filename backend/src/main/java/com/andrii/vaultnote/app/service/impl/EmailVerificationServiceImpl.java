package com.andrii.vaultnote.app.service.impl;

import com.andrii.vaultnote.app.config.EmailVerificationProperties;
import com.andrii.vaultnote.app.mail.MailMessage;
import com.andrii.vaultnote.app.mail.MailSender;
import com.andrii.vaultnote.app.security.EmailVerificationTokenGenerator;
import com.andrii.vaultnote.app.service.EmailVerificationService;
import com.andrii.vaultnote.users.infrastructure.persistence.entity.EmailVerificationTokenEntity;
import com.andrii.vaultnote.users.infrastructure.persistence.entity.UserEntity;
import com.andrii.vaultnote.users.infrastructure.persistence.repository.EmailVerificationTokenJpaRepository;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EmailVerificationServiceImpl implements EmailVerificationService {

  private static final String VERIFICATION_PATH = "/verify-email";
  private static final String EMAIL_SUBJECT = "Verify your VaultNote email";

  EmailVerificationTokenJpaRepository tokenRepository;
  EmailVerificationTokenGenerator tokenGenerator;
  EmailVerificationProperties properties;
  MailSender mailSender;

  @Override
  public void issueVerificationEmail(UserEntity user) {
    var generatedToken = tokenGenerator.generate();
    var token = EmailVerificationTokenEntity.builder()
        .user(user)
        .tokenHash(generatedToken.hash())
        .expiresAt(Instant.now().plus(properties.tokenTtl()))
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
        "Please verify your VaultNote email by opening this link:\n" + verificationUrl);

    mailSender.send(message);
  }
}
