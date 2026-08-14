package com.andrii.vaultnote.app.service;

import java.time.Instant;
import java.util.UUID;

public interface RefreshTokenFamilyRevocationService {

  int revokeActiveTokens(UUID tokenFamilyId, Instant revokedAt);

}
