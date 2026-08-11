package com.andrii.vaultnote.app.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.andrii.vaultnote.app.api.auth.dto.LoginResponse;
import com.andrii.vaultnote.app.api.auth.dto.TokenType;
import com.andrii.vaultnote.app.config.RefreshTokenProperties;
import com.andrii.vaultnote.app.exception.RefreshTokenAuthenticationFailedException;
import com.andrii.vaultnote.app.security.SecureTokenGenerator;
import com.andrii.vaultnote.app.service.AuthenticationResultFactory;
import com.andrii.vaultnote.app.service.LoginResult;
import com.andrii.vaultnote.users.infrastructure.persistence.entity.RefreshTokenEntity;
import com.andrii.vaultnote.users.infrastructure.persistence.entity.UserEntity;
import com.andrii.vaultnote.users.infrastructure.persistence.repository.RefreshTokenJpaRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@FieldDefaults(level = AccessLevel.PRIVATE)
class RefreshTokenServiceImplTest {

  private static final String RAW_REFRESH_TOKEN = "raw-refresh-token";
  private static final String REFRESH_TOKEN_HASH = "refresh-token-hash";
  private static final String NEXT_RAW_REFRESH_TOKEN = "next-raw-refresh-token";
  private static final String NEXT_REFRESH_TOKEN_HASH = "next-refresh-token-hash";
  private static final String ACCESS_TOKEN = "access-token";
  private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(7);
  private static final Instant NOW = Instant.parse("2026-08-10T12:00:00Z");

  @Mock
  SecureTokenGenerator secureTokenGenerator;
  @Mock
  RefreshTokenJpaRepository refreshTokenRepository;
  @Mock
  RefreshTokenProperties refreshTokenProperties;
  @Mock
  AuthenticationResultFactory authenticationResultFactory;
  @Mock
  Clock clock;

  @InjectMocks
  RefreshTokenServiceImpl refreshTokenService;

  @Test
  void shouldRotateRefreshToken() {
    var user = UserEntity.builder()
        .id(1L)
        .build();
    var tokenFamilyId = UUID.randomUUID();
    var token = RefreshTokenEntity.builder()
        .id(1L)
        .user(user)
        .tokenHash(REFRESH_TOKEN_HASH)
        .tokenFamilyId(tokenFamilyId)
        .expiresAt(NOW.plus(REFRESH_TOKEN_TTL))
        .build();
    var generatedRefreshToken = new SecureTokenGenerator.GeneratedToken(
        NEXT_RAW_REFRESH_TOKEN,
        NEXT_REFRESH_TOKEN_HASH);
    var expectedResult = new LoginResult(
        LoginResponse.builder()
            .accessToken(ACCESS_TOKEN)
            .tokenType(TokenType.BEARER)
            .expiresIn(900L)
            .build(),
        NEXT_RAW_REFRESH_TOKEN);

    when(secureTokenGenerator.hash(RAW_REFRESH_TOKEN)).thenReturn(REFRESH_TOKEN_HASH);
    when(refreshTokenRepository.findByTokenHash(REFRESH_TOKEN_HASH))
        .thenReturn(Optional.of(token));
    when(clock.instant()).thenReturn(NOW);
    when(refreshTokenRepository.revokeActiveById(token.getId(), NOW)).thenReturn(1);
    when(secureTokenGenerator.generate()).thenReturn(generatedRefreshToken);
    when(refreshTokenProperties.ttl()).thenReturn(REFRESH_TOKEN_TTL);
    when(authenticationResultFactory.create(user, NEXT_RAW_REFRESH_TOKEN))
        .thenReturn(expectedResult);

    var result = refreshTokenService.refresh(RAW_REFRESH_TOKEN);

    var refreshTokenCaptor = ArgumentCaptor.forClass(RefreshTokenEntity.class);
    verify(refreshTokenRepository).save(refreshTokenCaptor.capture());
    var savedRefreshToken = refreshTokenCaptor.getValue();

    assertThat(savedRefreshToken.getUser()).isSameAs(user);
    assertThat(savedRefreshToken.getTokenHash()).isEqualTo(NEXT_REFRESH_TOKEN_HASH);
    assertThat(savedRefreshToken.getTokenFamilyId()).isEqualTo(tokenFamilyId);
    assertThat(savedRefreshToken.getExpiresAt()).isEqualTo(NOW.plus(REFRESH_TOKEN_TTL));
    assertThat(result).isSameAs(expectedResult);

    verify(secureTokenGenerator).hash(RAW_REFRESH_TOKEN);
    verify(refreshTokenRepository).findByTokenHash(REFRESH_TOKEN_HASH);
    verify(refreshTokenRepository).revokeActiveById(token.getId(), NOW);
    verify(authenticationResultFactory).create(user, NEXT_RAW_REFRESH_TOKEN);
  }

  @Test
  void shouldRejectUnknownRefreshToken() {
    when(secureTokenGenerator.hash(RAW_REFRESH_TOKEN)).thenReturn(REFRESH_TOKEN_HASH);
    when(refreshTokenRepository.findByTokenHash(REFRESH_TOKEN_HASH))
        .thenReturn(Optional.empty());

    assertThatExceptionOfType(RefreshTokenAuthenticationFailedException.class)
        .isThrownBy(() -> refreshTokenService.refresh(RAW_REFRESH_TOKEN))
        .withMessage("Invalid or expired refresh token.");

    verify(secureTokenGenerator).hash(RAW_REFRESH_TOKEN);
    verify(secureTokenGenerator, never()).generate();
    verifyNoMoreInteractions(secureTokenGenerator);
    verify(refreshTokenRepository).findByTokenHash(REFRESH_TOKEN_HASH);
    verifyNoMoreInteractions(refreshTokenRepository);
    verifyNoInteractions(clock, refreshTokenProperties, authenticationResultFactory);
  }

  @Test
  void shouldRejectExpiredRefreshToken() {
    var token = RefreshTokenEntity.builder()
        .id(1L)
        .tokenHash(REFRESH_TOKEN_HASH)
        .tokenFamilyId(UUID.randomUUID())
        .expiresAt(NOW.minusSeconds(1))
        .build();
    when(secureTokenGenerator.hash(RAW_REFRESH_TOKEN)).thenReturn(REFRESH_TOKEN_HASH);
    when(refreshTokenRepository.findByTokenHash(REFRESH_TOKEN_HASH))
        .thenReturn(Optional.of(token));
    when(clock.instant()).thenReturn(NOW);

    assertThatExceptionOfType(RefreshTokenAuthenticationFailedException.class)
        .isThrownBy(() -> refreshTokenService.refresh(RAW_REFRESH_TOKEN))
        .withMessage("Invalid or expired refresh token.");

    verify(secureTokenGenerator).hash(RAW_REFRESH_TOKEN);
    verify(secureTokenGenerator, never()).generate();
    verifyNoMoreInteractions(secureTokenGenerator);
    verify(refreshTokenRepository).findByTokenHash(REFRESH_TOKEN_HASH);
    verifyNoMoreInteractions(refreshTokenRepository);
    verify(clock).instant();
    verifyNoInteractions(refreshTokenProperties, authenticationResultFactory);
  }

  @Test
  void shouldRevokeTokenFamilyWhenRefreshTokenIsReused() {
    var tokenFamilyId = UUID.randomUUID();
    var token = RefreshTokenEntity.builder()
        .id(1L)
        .tokenHash(REFRESH_TOKEN_HASH)
        .tokenFamilyId(tokenFamilyId)
        .expiresAt(NOW.plus(REFRESH_TOKEN_TTL))
        .revokedAt(NOW.minusSeconds(1))
        .build();
    when(secureTokenGenerator.hash(RAW_REFRESH_TOKEN)).thenReturn(REFRESH_TOKEN_HASH);
    when(refreshTokenRepository.findByTokenHash(REFRESH_TOKEN_HASH))
        .thenReturn(Optional.of(token));
    when(clock.instant()).thenReturn(NOW);
    when(refreshTokenRepository.revokeActiveByTokenFamilyId(tokenFamilyId, NOW))
        .thenReturn(1);

    assertThatExceptionOfType(RefreshTokenAuthenticationFailedException.class)
        .isThrownBy(() -> refreshTokenService.refresh(RAW_REFRESH_TOKEN))
        .withMessage("Invalid or expired refresh token.");

    verify(secureTokenGenerator).hash(RAW_REFRESH_TOKEN);
    verify(secureTokenGenerator, never()).generate();
    verifyNoMoreInteractions(secureTokenGenerator);
    verify(refreshTokenRepository).findByTokenHash(REFRESH_TOKEN_HASH);
    verify(refreshTokenRepository).revokeActiveByTokenFamilyId(tokenFamilyId, NOW);
    verifyNoMoreInteractions(refreshTokenRepository);
    verify(clock).instant();
    verifyNoInteractions(refreshTokenProperties, authenticationResultFactory);
  }

  @Test
  void shouldRevokeActiveRefreshTokenOnLogout() {
    var token = RefreshTokenEntity.builder()
        .id(1L)
        .tokenHash(REFRESH_TOKEN_HASH)
        .expiresAt(NOW.plus(REFRESH_TOKEN_TTL))
        .build();
    when(secureTokenGenerator.hash(RAW_REFRESH_TOKEN)).thenReturn(REFRESH_TOKEN_HASH);
    when(refreshTokenRepository.findByTokenHash(REFRESH_TOKEN_HASH))
        .thenReturn(Optional.of(token));
    when(clock.instant()).thenReturn(NOW);
    when(refreshTokenRepository.revokeActiveById(token.getId(), NOW)).thenReturn(1);

    refreshTokenService.logout(RAW_REFRESH_TOKEN);

    verify(secureTokenGenerator).hash(RAW_REFRESH_TOKEN);
    verify(refreshTokenRepository).findByTokenHash(REFRESH_TOKEN_HASH);
    verify(clock).instant();
    verify(refreshTokenRepository).revokeActiveById(token.getId(), NOW);
    verifyNoMoreInteractions(secureTokenGenerator, refreshTokenRepository, clock);
    verifyNoInteractions(refreshTokenProperties, authenticationResultFactory);
  }

  @Test
  void shouldIgnoreUnknownRefreshTokenOnLogout() {
    when(secureTokenGenerator.hash(RAW_REFRESH_TOKEN)).thenReturn(REFRESH_TOKEN_HASH);
    when(refreshTokenRepository.findByTokenHash(REFRESH_TOKEN_HASH))
        .thenReturn(Optional.empty());

    refreshTokenService.logout(RAW_REFRESH_TOKEN);

    verify(secureTokenGenerator).hash(RAW_REFRESH_TOKEN);
    verify(refreshTokenRepository).findByTokenHash(REFRESH_TOKEN_HASH);
    verifyNoMoreInteractions(secureTokenGenerator, refreshTokenRepository);
    verifyNoInteractions(clock, refreshTokenProperties, authenticationResultFactory);
  }

  @Test
  void shouldIgnoreAlreadyRevokedRefreshTokenOnLogout() {
    var token = RefreshTokenEntity.builder()
        .id(1L)
        .tokenHash(REFRESH_TOKEN_HASH)
        .expiresAt(NOW.plus(REFRESH_TOKEN_TTL))
        .revokedAt(NOW.minusSeconds(1))
        .build();
    when(secureTokenGenerator.hash(RAW_REFRESH_TOKEN)).thenReturn(REFRESH_TOKEN_HASH);
    when(refreshTokenRepository.findByTokenHash(REFRESH_TOKEN_HASH))
        .thenReturn(Optional.of(token));

    refreshTokenService.logout(RAW_REFRESH_TOKEN);

    verify(secureTokenGenerator).hash(RAW_REFRESH_TOKEN);
    verify(refreshTokenRepository).findByTokenHash(REFRESH_TOKEN_HASH);
    verifyNoMoreInteractions(secureTokenGenerator, refreshTokenRepository);
    verifyNoInteractions(clock, refreshTokenProperties, authenticationResultFactory);
  }

  @Test
  void shouldIgnoreMissingRefreshTokenOnLogout() {
    refreshTokenService.logout(null);

    verifyNoInteractions(
        secureTokenGenerator,
        refreshTokenRepository,
        refreshTokenProperties,
        authenticationResultFactory,
        clock);
  }
}
