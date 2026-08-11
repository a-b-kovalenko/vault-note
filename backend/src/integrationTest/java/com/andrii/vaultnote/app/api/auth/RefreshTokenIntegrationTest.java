package com.andrii.vaultnote.app.api.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.andrii.vaultnote.app.api.auth.dto.LoginRequest;
import com.andrii.vaultnote.app.api.auth.dto.LoginResponse;
import com.andrii.vaultnote.app.api.auth.dto.TokenType;
import com.andrii.vaultnote.app.security.SecureTokenGenerator;
import com.andrii.vaultnote.support.AbstractBaseIntegrationTest;
import com.andrii.vaultnote.users.domain.UserRole;
import com.andrii.vaultnote.users.infrastructure.persistence.entity.UserEntity;
import com.andrii.vaultnote.users.infrastructure.persistence.repository.RefreshTokenJpaRepository;
import com.andrii.vaultnote.users.infrastructure.persistence.repository.UserJpaRepository;
import com.github.database.rider.core.api.dataset.DataSet;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

@DataSet(value = "auth-baseline.yml", skipCleaningFor = {"databasechangelog", "databasechangeloglock"})
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
class RefreshTokenIntegrationTest extends AbstractBaseIntegrationTest {

  private static final String LOGIN_ENDPOINT = "/api/v1/auth/login";
  private static final String REFRESH_ENDPOINT = "/api/v1/auth/refresh";
  private static final String LOGOUT_ENDPOINT = "/api/v1/auth/logout";
  private static final String REFRESH_COOKIE_NAME = "vaultnote_refresh_token";
  private static final String USER_EMAIL = "refresh@example.com";
  private static final String PASSWORD = "Password1234";
  private static final Duration ACCESS_TOKEN_TTL = Duration.ofMinutes(15);
  private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(7);

  UserJpaRepository userRepository;
  RefreshTokenJpaRepository refreshTokenRepository;
  PasswordEncoder passwordEncoder;
  SecureTokenGenerator secureTokenGenerator;

  @Autowired
  RefreshTokenIntegrationTest(
      UserJpaRepository userRepository,
      RefreshTokenJpaRepository refreshTokenRepository,
      PasswordEncoder passwordEncoder,
      SecureTokenGenerator secureTokenGenerator) {
    this.userRepository = userRepository;
    this.refreshTokenRepository = refreshTokenRepository;
    this.passwordEncoder = passwordEncoder;
    this.secureTokenGenerator = secureTokenGenerator;
  }

  /**
   * Rotates a refresh token through the HTTP endpoint and persists the new token
   * in the same family.
   *
   * <p>
   * The previous token must be revoked, while the replacement remains active and
   * receives a new raw value in the HttpOnly cookie.
   */
  @Test
  void shouldRotateRefreshTokenAgainstPostgres() {
    var user = saveUser();
    var loginRequest = LoginRequest.builder()
        .email(USER_EMAIL)
        .password(PASSWORD)
        .build();

    var loginResponse = givenWithCsrf()
        .port(port)
        .contentType("application/json")
        .body(loginRequest)
        .when()
        .post(LOGIN_ENDPOINT)
        .then()
        .statusCode(HttpStatus.OK.value())
        .extract()
        .response();

    var oldRawRefreshToken = loginResponse.getCookie(REFRESH_COOKIE_NAME);
    var oldRefreshTokenHash = secureTokenGenerator.hash(oldRawRefreshToken);
    var requestStartedAt = Instant.now();

    var response = givenWithCsrf()
        .port(port)
        .cookie(REFRESH_COOKIE_NAME, oldRawRefreshToken)
        .when()
        .post(REFRESH_ENDPOINT)
        .then()
        .statusCode(HttpStatus.OK.value())
        .extract()
        .response();

    var requestFinishedAt = Instant.now();
    var refreshResponse = response.as(LoginResponse.class);
    var newRawRefreshToken = response.getCookie(REFRESH_COOKIE_NAME);
    var persistedTokens = refreshTokenRepository.findAll();
    var newRefreshToken = persistedTokens.stream()
        .filter(token -> token.getTokenHash().equals(secureTokenGenerator.hash(newRawRefreshToken)))
        .findFirst()
        .orElseThrow();
    var revokedRefreshToken = persistedTokens.stream()
        .filter(token -> token.getTokenHash().equals(oldRefreshTokenHash))
        .findFirst()
        .orElseThrow();

    assertThat(refreshResponse.accessToken())
        .isNotBlank()
        .satisfies(token -> assertThat(token.split("\\.")).hasSize(3));
    assertThat(refreshResponse.tokenType()).isEqualTo(TokenType.BEARER);
    assertThat(refreshResponse.expiresIn()).isEqualTo(ACCESS_TOKEN_TTL.toSeconds());
    assertThat(newRawRefreshToken)
        .isNotBlank()
        .isNotEqualTo(oldRawRefreshToken);
    assertThat(response.getHeader("Set-Cookie"))
        .contains(REFRESH_COOKIE_NAME + "=" + newRawRefreshToken)
        .contains("Path=/api/v1/auth")
        .contains("Max-Age=" + REFRESH_TOKEN_TTL.toSeconds())
        .contains("HttpOnly")
        .contains("SameSite=Lax");

    assertThat(persistedTokens).hasSize(2);
    assertThat(revokedRefreshToken.getUser().getId()).isEqualTo(user.getId());
    assertThat(revokedRefreshToken.getRevokedAt()).isNotNull();
    assertThat(newRefreshToken.getUser().getId()).isEqualTo(user.getId());
    assertThat(newRefreshToken.getRevokedAt()).isNull();
    assertThat(newRefreshToken.getTokenFamilyId())
        .isEqualTo(revokedRefreshToken.getTokenFamilyId());
    assertThat(newRefreshToken.getExpiresAt())
        .isAfter(requestStartedAt.plus(REFRESH_TOKEN_TTL).minusSeconds(5))
        .isBefore(requestFinishedAt.plus(REFRESH_TOKEN_TTL).plusSeconds(5));
  }

  /**
   * Revokes the current refresh token through the HTTP logout endpoint and clears
   * the refresh-token cookie.
   */
  @Test
  void shouldLogoutAndClearRefreshTokenAgainstPostgres() {
    var user = saveUser();
    var loginRequest = LoginRequest.builder()
        .email(USER_EMAIL)
        .password(PASSWORD)
        .build();
    var loginResponse = givenWithCsrf()
        .port(port)
        .contentType("application/json")
        .body(loginRequest)
        .when()
        .post(LOGIN_ENDPOINT)
        .then()
        .statusCode(HttpStatus.OK.value())
        .extract()
        .response();
    var rawRefreshToken = loginResponse.getCookie(REFRESH_COOKIE_NAME);
    var persistedToken = refreshTokenRepository.findAll().stream()
        .findFirst()
        .orElseThrow();

    var response = givenWithCsrf()
        .port(port)
        .cookie(REFRESH_COOKIE_NAME, rawRefreshToken)
        .when()
        .post(LOGOUT_ENDPOINT)
        .then()
        .statusCode(HttpStatus.NO_CONTENT.value())
        .extract()
        .response();

    var revokedToken = refreshTokenRepository.findById(persistedToken.getId()).orElseThrow();
    assertThat(response.getBody().asString()).isEmpty();
    assertThat(response.getHeader("Set-Cookie"))
        .contains(REFRESH_COOKIE_NAME + "=")
        .contains("Path=/api/v1/auth")
        .contains("Max-Age=0")
        .contains("HttpOnly")
        .contains("SameSite=Lax");
    assertThat(revokedToken.getUser().getId()).isEqualTo(user.getId());
    assertThat(revokedToken.getRevokedAt()).isNotNull();
  }

  /**
   * Clears the refresh-token cookie even when the client has no active cookie.
   */
  @Test
  void shouldClearRefreshTokenCookieWhenLogoutCookieIsMissing() {
    var response = givenWithCsrf()
        .port(port)
        .when()
        .post(LOGOUT_ENDPOINT)
        .then()
        .statusCode(HttpStatus.NO_CONTENT.value())
        .extract()
        .response();

    assertThat(response.getHeader("Set-Cookie"))
        .contains(REFRESH_COOKIE_NAME + "=")
        .contains("Path=/api/v1/auth")
        .contains("Max-Age=0")
        .contains("HttpOnly")
        .contains("SameSite=Lax");
    assertThat(refreshTokenRepository.findAll()).isEmpty();
  }

  private UserEntity saveUser() {
    return userRepository.saveAndFlush(UserEntity.builder()
        .email(USER_EMAIL)
        .displayName("Refresh User")
        .passwordHash(passwordEncoder.encode(PASSWORD))
        .emailVerified(true)
        .roles(EnumSet.of(UserRole.USER))
        .build());
  }
}
