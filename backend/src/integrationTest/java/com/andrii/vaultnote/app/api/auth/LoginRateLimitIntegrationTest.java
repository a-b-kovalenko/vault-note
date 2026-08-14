package com.andrii.vaultnote.app.api.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.andrii.vaultnote.app.api.auth.dto.LoginRequest;
import com.andrii.vaultnote.app.api.error.ApiErrorResponse;
import com.andrii.vaultnote.support.AbstractBaseIntegrationTest;
import com.andrii.vaultnote.users.domain.UserRole;
import com.andrii.vaultnote.users.infrastructure.persistence.entity.UserEntity;
import com.andrii.vaultnote.users.infrastructure.persistence.repository.RefreshTokenJpaRepository;
import com.andrii.vaultnote.users.infrastructure.persistence.repository.UserJpaRepository;
import com.github.database.rider.core.api.dataset.DataSet;
import io.restassured.http.ContentType;
import java.util.EnumSet;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.security.crypto.password.PasswordEncoder;

@DataSet(value = "auth-baseline.yml", skipCleaningFor = {"databasechangelog", "databasechangeloglock"})
@TestPropertySource(
  properties = {
    "app.security.rate-limit.login.ip-limit=100",
    "app.security.rate-limit.login.email-limit=2",
    "app.security.rate-limit.login.window=PT1M"
  })
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
class LoginRateLimitIntegrationTest extends AbstractBaseIntegrationTest {

  private static final String LOGIN_ENDPOINT = "/api/v1/auth/login";
  private static final String EMAIL = "rate-limit@example.com";
  private static final String PASSWORD = "Password1234";
  private static final String WRONG_PASSWORD = "WrongPassword1234";

  UserJpaRepository userRepository;
  RefreshTokenJpaRepository refreshTokenRepository;
  PasswordEncoder passwordEncoder;

  @Autowired
  LoginRateLimitIntegrationTest(
    UserJpaRepository userRepository,
    RefreshTokenJpaRepository refreshTokenRepository,
    PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.refreshTokenRepository = refreshTokenRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @Test
  void shouldRejectLoginAfterEmailLimitBeforeCreatingRefreshToken() {
    userRepository.saveAndFlush(UserEntity.builder()
      .email(EMAIL)
      .displayName("Rate Limit User")
      .passwordHash(passwordEncoder.encode(PASSWORD))
      .emailVerified(true)
      .roles(EnumSet.of(UserRole.USER))
      .build());
    var request = LoginRequest.builder()
      .email(EMAIL)
      .password(WRONG_PASSWORD)
      .build();

    for (var attempt = 0; attempt < 2; attempt++) {
      givenWithCsrf()
        .port(port)
        .contentType(ContentType.JSON)
        .body(request)
        .when()
        .post(LOGIN_ENDPOINT)
        .then()
        .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    var response = givenWithCsrf()
      .port(port)
      .contentType(ContentType.JSON)
      .body(request)
      .when()
      .post(LOGIN_ENDPOINT)
      .then()
      .statusCode(HttpStatus.TOO_MANY_REQUESTS.value())
      .extract()
      .response();

    var errorResponse = response.as(ApiErrorResponse.class);
    var retryAfter = Long.parseLong(response.getHeader("Retry-After"));

    assertThat(errorResponse.code()).isEqualTo("RATE_LIMIT_EXCEEDED");
    assertThat(retryAfter).isBetween(1L, 60L);
    assertThat(refreshTokenRepository.findAll()).isEmpty();
  }
}
