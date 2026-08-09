package com.andrii.vaultnote.app.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.andrii.vaultnote.app.api.auth.dto.LoginRequest;
import com.andrii.vaultnote.app.api.auth.dto.TokenType;
import com.andrii.vaultnote.app.config.RefreshTokenProperties;
import com.andrii.vaultnote.app.exception.AuthenticationFailedException;
import com.andrii.vaultnote.app.security.AccessTokenGenerator;
import com.andrii.vaultnote.app.security.SecureTokenGenerator;
import com.andrii.vaultnote.users.domain.UserRole;
import com.andrii.vaultnote.users.infrastructure.persistence.entity.RefreshTokenEntity;
import com.andrii.vaultnote.users.infrastructure.persistence.entity.UserEntity;
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
class LoginServiceImplTest {

  private static final String EMAIL = "user@example.com";
  private static final String PASSWORD = "Password1234";
  private static final String PASSWORD_HASH = "password-hash";
  private static final String ACCESS_TOKEN = "access-token";
  private static final String RAW_REFRESH_TOKEN = "raw-refresh-token";
  private static final String REFRESH_TOKEN_HASH = "refresh-token-hash";
  private static final Duration ACCESS_TOKEN_TTL = Duration.ofMinutes(15);
  private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(7);
  private static final Instant NOW = Instant.parse("2026-08-10T12:00:00Z");

  @Mock
  UserJpaRepository userRepository;
  @Mock
  RefreshTokenJpaRepository refreshTokenRepository;
  @Mock
  PasswordEncoder passwordEncoder;
  @Mock
  AccessTokenGenerator accessTokenGenerator;
  @Mock
  SecureTokenGenerator secureTokenGenerator;
  @Mock
  RefreshTokenProperties refreshTokenProperties;
  @Mock
  Clock clock;

  @InjectMocks
  LoginServiceImpl loginService;

  @Test
  void shouldLoginVerifiedUserAndPersistRefreshToken() {
    var user = verifiedUser();
    var request = loginRequest();
    var generatedRefreshToken = new SecureTokenGenerator.GeneratedToken(
        RAW_REFRESH_TOKEN, REFRESH_TOKEN_HASH);
    var generatedAccessToken = new AccessTokenGenerator.GeneratedToken(
        ACCESS_TOKEN, ACCESS_TOKEN_TTL.toSeconds());

    when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
    when(passwordEncoder.matches(PASSWORD, PASSWORD_HASH)).thenReturn(true);
    when(secureTokenGenerator.generate()).thenReturn(generatedRefreshToken);
    when(accessTokenGenerator.generate(user)).thenReturn(generatedAccessToken);
    when(clock.instant()).thenReturn(NOW);
    when(refreshTokenProperties.ttl()).thenReturn(REFRESH_TOKEN_TTL);

    var result = loginService.login(request);

    var refreshTokenCaptor = ArgumentCaptor.forClass(RefreshTokenEntity.class);
    verify(refreshTokenRepository).save(refreshTokenCaptor.capture());
    var savedRefreshToken = refreshTokenCaptor.getValue();

    assertThat(savedRefreshToken.getUser()).isSameAs(user);
    assertThat(savedRefreshToken.getTokenHash()).isEqualTo(REFRESH_TOKEN_HASH);
    assertThat(savedRefreshToken.getTokenFamilyId()).isNotNull();
    assertThat(savedRefreshToken.getExpiresAt()).isEqualTo(NOW.plus(REFRESH_TOKEN_TTL));
    assertThat(result.rawRefreshToken()).isEqualTo(RAW_REFRESH_TOKEN);
    assertThat(result.response().accessToken()).isEqualTo(ACCESS_TOKEN);
    assertThat(result.response().tokenType()).isEqualTo(TokenType.BEARER);
    assertThat(result.response().expiresIn()).isEqualTo(ACCESS_TOKEN_TTL.toSeconds());

    verify(passwordEncoder).matches(PASSWORD, PASSWORD_HASH);
    verify(secureTokenGenerator).generate();
    verify(accessTokenGenerator).generate(user);
  }

  @Test
  void shouldRejectMissingUser() {
    when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

    assertThatExceptionOfType(AuthenticationFailedException.class)
        .isThrownBy(() -> loginService.login(loginRequest()))
        .withMessage("Invalid email or password.");

    verifyNoInteractions(
        passwordEncoder,
        secureTokenGenerator,
        refreshTokenRepository,
        accessTokenGenerator,
        refreshTokenProperties,
        clock);
  }

  @Test
  void shouldRejectUnverifiedUser() {
    var user = verifiedUser().toBuilder()
        .emailVerified(false)
        .build();
    when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

    assertThatExceptionOfType(AuthenticationFailedException.class)
        .isThrownBy(() -> loginService.login(loginRequest()))
        .withMessage("Invalid email or password.");

    verifyNoInteractions(
        passwordEncoder,
        secureTokenGenerator,
        refreshTokenRepository,
        accessTokenGenerator,
        refreshTokenProperties,
        clock);
  }

  @Test
  void shouldRejectInvalidPassword() {
    var user = verifiedUser();
    when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
    when(passwordEncoder.matches(PASSWORD, PASSWORD_HASH)).thenReturn(false);

    assertThatExceptionOfType(AuthenticationFailedException.class)
        .isThrownBy(() -> loginService.login(loginRequest()))
        .withMessage("Invalid email or password.");

    verify(passwordEncoder).matches(PASSWORD, PASSWORD_HASH);
    verifyNoInteractions(
        secureTokenGenerator,
        refreshTokenRepository,
        accessTokenGenerator,
        refreshTokenProperties,
        clock);
    verify(refreshTokenRepository, never()).save(any(RefreshTokenEntity.class));
  }

  private static LoginRequest loginRequest() {
    return LoginRequest.builder()
        .email(EMAIL)
        .password(PASSWORD)
        .build();
  }

  private static UserEntity verifiedUser() {
    return UserEntity.builder()
        .id(1L)
        .email(EMAIL)
        .passwordHash(PASSWORD_HASH)
        .emailVerified(true)
        .roles(EnumSet.of(UserRole.USER))
        .build();
  }
}
