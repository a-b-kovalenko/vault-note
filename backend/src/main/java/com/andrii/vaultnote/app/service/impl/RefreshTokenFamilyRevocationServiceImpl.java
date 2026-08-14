package com.andrii.vaultnote.app.service.impl;

import com.andrii.vaultnote.app.service.RefreshTokenFamilyRevocationService;
import com.andrii.vaultnote.users.infrastructure.persistence.repository.RefreshTokenJpaRepository;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RefreshTokenFamilyRevocationServiceImpl implements RefreshTokenFamilyRevocationService {

  RefreshTokenJpaRepository refreshTokenRepository;

  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public int revokeActiveTokens(UUID tokenFamilyId, Instant revokedAt) {
    log.info("Revoking active refresh tokens for family {}", tokenFamilyId);
    return refreshTokenRepository.revokeActiveByTokenFamilyId(tokenFamilyId, revokedAt);
  }

}
