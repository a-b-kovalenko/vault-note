package com.andrii.vaultnote.app.api.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.andrii.vaultnote.app.api.auth.dto.LoginRequest;
import com.andrii.vaultnote.app.api.auth.dto.LoginResponse;
import com.andrii.vaultnote.app.api.auth.dto.TokenType;
import com.andrii.vaultnote.app.api.error.ApiErrorResponse;
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
class LoginIntegrationTest extends AbstractBaseIntegrationTest {

  private static final String LOGIN_ENDPOINT = "/api/v1/auth/login";
  private static final String USER_EMAIL = "login@example.com";
  private static final String UNVERIFIED_USER_EMAIL = "unverified-login@example.com";
  private static final String PASSWORD = "Password1234";
  private static final String WRONG_PASSWORD = "WrongPassword1234";
  private static final String REFRESH_COOKIE_NAME = "vaultnote_refresh_token";
  private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(7);

  UserJpaRepository userRepository;
  RefreshTokenJpaRepository refreshTokenRepository;
  PasswordEncoder passwordEncoder;
  SecureTokenGenerator secureTokenGenerator;

  @Autowired
  LoginIntegrationTest(
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
   * Logs in a verified user against PostgreSQL.
   *
   * <p>
   * The endpoint must return a JWT access token, set a seven-day HttpOnly
   * refresh-token cookie, and persist only the refresh-token hash.
   */
  @Test
  void shouldLoginVerifiedUserAgainstPostgres() {
    var user = userRepository.saveAndFlush(UserEntity.builder()
        .email(USER_EMAIL)
        .displayName("Login User")
        .passwordHash(passwordEncoder.encode(PASSWORD))
        .emailVerified(true)
        .roles(EnumSet.of(UserRole.USER))
        .build());
    var request = LoginRequest.builder()
        .email(USER_EMAIL)
        .password(PASSWORD)
        .build();
    var requestStartedAt = Instant.now();

    var response = givenWithCsrf()
        .port(port)
        .contentType("application/json")
        .body(request)
        .when()
        .post(LOGIN_ENDPOINT)
        .then()
        .statusCode(HttpStatus.OK.value())
        .extract()
        .response();

    var requestFinishedAt = Instant.now();
    var loginResponse = response.as(LoginResponse.class);
    var rawRefreshToken = response.getCookie(REFRESH_COOKIE_NAME);
    var setCookieHeader = response.getHeader("Set-Cookie");

    assertThat(loginResponse.accessToken())
        .isNotBlank()
        .satisfies(token -> assertThat(token.split("\\.")).hasSize(3));
    assertThat(loginResponse.tokenType()).isEqualTo(TokenType.BEARER);
    assertThat(loginResponse.expiresIn()).isEqualTo(Duration.ofMinutes(15).toSeconds());
    assertThat(rawRefreshToken).isNotBlank();
    assertThat(setCookieHeader)
        .contains(REFRESH_COOKIE_NAME + "=" + rawRefreshToken)
        .contains("Path=/api/v1/auth")
        .contains("Max-Age=" + REFRESH_TOKEN_TTL.toSeconds())
        .contains("HttpOnly")
        .contains("SameSite=Lax");

    assertThat(refreshTokenRepository.findAll())
        .singleElement()
        .satisfies(refreshToken -> {
          assertThat(refreshToken.getUser().getId()).isEqualTo(user.getId());
          assertThat(refreshToken.getTokenHash())
              .isEqualTo(secureTokenGenerator.hash(rawRefreshToken));
          assertThat(refreshToken.getTokenHash()).isNotEqualTo(rawRefreshToken);
          assertThat(refreshToken.getTokenFamilyId()).isNotNull();
          assertThat(refreshToken.getExpiresAt())
              .isAfter(requestStartedAt.plus(REFRESH_TOKEN_TTL).minusSeconds(5))
              .isBefore(requestFinishedAt.plus(REFRESH_TOKEN_TTL).plusSeconds(5));
          assertThat(refreshToken.getRevokedAt()).isNull();
        });
  }

  /**
   * Rejects a verified user's login when the password is incorrect.
   *
   * <p>
   * The endpoint must return {@code 401 Unauthorized}, avoid setting a
   * refresh-token cookie, and leave the refresh-token table unchanged.
   */
  @Test
  void shouldRejectInvalidPasswordAgainstPostgres() {
    userRepository.saveAndFlush(UserEntity.builder()
        .email(USER_EMAIL)
        .displayName("Login User")
        .passwordHash(passwordEncoder.encode(PASSWORD))
        .emailVerified(true)
        .roles(EnumSet.of(UserRole.USER))
        .build());
    var request = LoginRequest.builder()
        .email(USER_EMAIL)
        .password(WRONG_PASSWORD)
        .build();

    var response = givenWithCsrf()
        .port(port)
        .contentType("application/json")
        .body(request)
        .when()
        .post(LOGIN_ENDPOINT)
        .then()
        .statusCode(HttpStatus.UNAUTHORIZED.value())
        .extract()
        .response();

    var errorResponse = response.as(ApiErrorResponse.class);

    assertThat(errorResponse.code()).isEqualTo("AUTHENTICATION_FAILED");
    assertThat(response.getHeader("Set-Cookie")).isNull();
    assertThat(refreshTokenRepository.findAll()).isEmpty();
  }

  /**
   * Rejects a user's login when the email address has not been verified.
   *
   * <p>
   * The endpoint must return the same neutral {@code 401 Unauthorized} as an
   * invalid password and must not create a refresh-token session.
   */
  @Test
  void shouldRejectUnverifiedUserAgainstPostgres() {
    userRepository.saveAndFlush(UserEntity.builder()
        .email(UNVERIFIED_USER_EMAIL)
        .displayName("Unverified Login User")
        .passwordHash(passwordEncoder.encode(PASSWORD))
        .emailVerified(false)
        .roles(EnumSet.of(UserRole.USER))
        .build());
    var request = LoginRequest.builder()
        .email(UNVERIFIED_USER_EMAIL)
        .password(PASSWORD)
        .build();

    var response = givenWithCsrf()
        .port(port)
        .contentType("application/json")
        .body(request)
        .when()
        .post(LOGIN_ENDPOINT)
        .then()
        .statusCode(HttpStatus.UNAUTHORIZED.value())
        .extract()
        .response();

    var errorResponse = response.as(ApiErrorResponse.class);

    assertThat(errorResponse.code()).isEqualTo("AUTHENTICATION_FAILED");
    assertThat(response.getHeader("Set-Cookie")).isNull();
    assertThat(refreshTokenRepository.findAll()).isEmpty();
  }
}
