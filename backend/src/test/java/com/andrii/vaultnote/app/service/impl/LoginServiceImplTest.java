package com.andrii.vaultnote.app.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.andrii.vaultnote.app.api.auth.dto.LoginRequest;
import com.andrii.vaultnote.app.api.auth.dto.LoginResponse;
import com.andrii.vaultnote.app.api.auth.dto.TokenType;
import com.andrii.vaultnote.app.exception.AuthenticationFailedException;
import com.andrii.vaultnote.app.exception.RateLimitExceededException;
import com.andrii.vaultnote.app.security.ratelimit.RateLimitScope;
import com.andrii.vaultnote.app.service.AuthenticationResultFactory;
import com.andrii.vaultnote.app.service.LoginResult;
import com.andrii.vaultnote.app.service.RateLimitService;
import com.andrii.vaultnote.app.service.RefreshTokenService;
import com.andrii.vaultnote.users.domain.UserRole;
import com.andrii.vaultnote.users.infrastructure.persistence.entity.UserEntity;
import com.andrii.vaultnote.users.infrastructure.persistence.repository.UserJpaRepository;
import java.time.Duration;
import java.util.EnumSet;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
  private static final Duration ACCESS_TOKEN_TTL = Duration.ofMinutes(15);

  @Mock
  UserJpaRepository userRepository;
  @Mock
  PasswordEncoder passwordEncoder;
  @Mock
  AuthenticationResultFactory authenticationResultFactory;
  @Mock
  RefreshTokenService refreshTokenService;
  @Mock
  RateLimitService rateLimitService;

  @InjectMocks
  LoginServiceImpl loginService;

  @Test
  void shouldLoginVerifiedUserAndPersistRefreshToken() {
    var user = verifiedUser();
    var request = loginRequest();
    var expectedResult = new LoginResult(
      LoginResponse.builder()
        .accessToken(ACCESS_TOKEN)
        .tokenType(TokenType.BEARER)
        .expiresIn(ACCESS_TOKEN_TTL.toSeconds())
        .build(),
      RAW_REFRESH_TOKEN);

    when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
    when(passwordEncoder.matches(PASSWORD, PASSWORD_HASH)).thenReturn(true);
    when(refreshTokenService.createSession(user)).thenReturn(RAW_REFRESH_TOKEN);
    when(authenticationResultFactory.create(user, RAW_REFRESH_TOKEN)).thenReturn(expectedResult);

    var result = loginService.login(request, "127.0.0.1");

    assertThat(result).isSameAs(expectedResult);

    verify(passwordEncoder).matches(PASSWORD, PASSWORD_HASH);
    verify(rateLimitService).check(RateLimitScope.LOGIN, "127.0.0.1", EMAIL);
    verify(refreshTokenService).createSession(user);
    verify(authenticationResultFactory).create(user, RAW_REFRESH_TOKEN);
  }

  @Test
  void shouldRejectBeforeLookingUpUserWhenRateLimitIsExceeded() {
    var exception = new RateLimitExceededException(Duration.ofSeconds(30));
    doThrow(exception)
      .when(rateLimitService)
      .check(RateLimitScope.LOGIN, "127.0.0.1", EMAIL);

    assertThatExceptionOfType(RateLimitExceededException.class)
      .isThrownBy(() -> loginService.login(loginRequest(), "127.0.0.1"))
      .isSameAs(exception);

    verify(rateLimitService).check(RateLimitScope.LOGIN, "127.0.0.1", EMAIL);
    verifyNoInteractions(
      userRepository,
      passwordEncoder,
      refreshTokenService,
      authenticationResultFactory);
  }

  @Test
  void shouldRejectMissingUser() {
    when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

    assertThatExceptionOfType(AuthenticationFailedException.class)
      .isThrownBy(() -> loginService.login(loginRequest(), "127.0.0.1"))
      .withMessage("Invalid email or password.");

    verifyNoInteractions(
      passwordEncoder,
      refreshTokenService,
      authenticationResultFactory);
  }

  @Test
  void shouldRejectUnverifiedUser() {
    var user = verifiedUser().toBuilder()
      .emailVerified(false)
      .build();
    when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

    assertThatExceptionOfType(AuthenticationFailedException.class)
      .isThrownBy(() -> loginService.login(loginRequest(), "127.0.0.1"))
      .withMessage("Invalid email or password.");

    verifyNoInteractions(
      passwordEncoder,
      refreshTokenService,
      authenticationResultFactory);
  }

  @Test
  void shouldRejectInvalidPassword() {
    var user = verifiedUser();
    when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
    when(passwordEncoder.matches(PASSWORD, PASSWORD_HASH)).thenReturn(false);

    assertThatExceptionOfType(AuthenticationFailedException.class)
      .isThrownBy(() -> loginService.login(loginRequest(), "127.0.0.1"))
      .withMessage("Invalid email or password.");

    verify(passwordEncoder).matches(PASSWORD, PASSWORD_HASH);
    verifyNoInteractions(
      refreshTokenService,
      authenticationResultFactory);
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
