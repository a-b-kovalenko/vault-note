package com.andrii.vaultnote.app.service.impl;

import com.andrii.vaultnote.app.config.RefreshTokenProperties;
import com.andrii.vaultnote.app.exception.RefreshTokenAuthenticationFailedException;
import com.andrii.vaultnote.app.security.SecureTokenGenerator;
import com.andrii.vaultnote.app.service.AuthenticationResultFactory;
import com.andrii.vaultnote.app.service.LoginResult;
import com.andrii.vaultnote.app.service.RefreshTokenService;
import com.andrii.vaultnote.users.infrastructure.persistence.entity.RefreshTokenEntity;
import com.andrii.vaultnote.users.infrastructure.persistence.repository.RefreshTokenJpaRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RefreshTokenServiceImpl implements RefreshTokenService {

  SecureTokenGenerator secureTokenGenerator;
  AuthenticationResultFactory authenticationResultFactory;
  RefreshTokenJpaRepository refreshTokenRepository;
  RefreshTokenProperties refreshTokenProperties;
  Clock clock;

  @Override
  @Transactional
  public LoginResult refresh(String rawRefreshToken) {
    log.info("Received request to refresh access token");

    var tokenHash = secureTokenGenerator.hash(rawRefreshToken);
    var token = refreshTokenRepository.findByTokenHash(tokenHash)
        .orElseThrow(RefreshTokenAuthenticationFailedException::new);

    var now = clock.instant();

    if (nonNull(token.getRevokedAt())) {
      log.warn("Refresh token reuse detected for family {}", token.getTokenFamilyId());
      refreshTokenRepository.revokeActiveByTokenFamilyId(token.getTokenFamilyId(), now);
      throw new RefreshTokenAuthenticationFailedException();
    }

    if (!token.getExpiresAt().isAfter(now)) {
      throw new RefreshTokenAuthenticationFailedException();
    }
    var revokedTokens = refreshTokenRepository.revokeActiveById(token.getId(), now);
    if (revokedTokens != 1) {
      throw new RefreshTokenAuthenticationFailedException();
    }

    var generatedRefreshToken = secureTokenGenerator.generate();
    var nextRefreshToken = RefreshTokenEntity.builder()
        .user(token.getUser())
        .tokenHash(generatedRefreshToken.hash())
        .tokenFamilyId(token.getTokenFamilyId())
        .expiresAt(now.plus(refreshTokenProperties.ttl()))
        .build();
    refreshTokenRepository.save(nextRefreshToken);

    return authenticationResultFactory.create(token.getUser(), generatedRefreshToken.rawValue());
  }

  @Override
  @Transactional
  public void logout(String rawRefreshToken) {
    log.info("Received request to logout");

    if (isNull(rawRefreshToken)) {
      log.info("Logout request did not contain a refresh token");
      return;
    }

    var tokenHash = secureTokenGenerator.hash(rawRefreshToken);
    var revokedTokens = refreshTokenRepository.findByTokenHash(tokenHash)
        .filter(token -> isNull(token.getRevokedAt()))
        .map(token -> refreshTokenRepository.revokeActiveById(token.getId(), clock.instant()))
        .orElse(0);

    log.info("Logout completed; revoked {} refresh token(s)", revokedTokens);
  }

}
