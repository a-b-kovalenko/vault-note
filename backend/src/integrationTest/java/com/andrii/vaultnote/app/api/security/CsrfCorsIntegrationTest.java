package com.andrii.vaultnote.app.api.security;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import com.andrii.vaultnote.app.api.auth.dto.LoginRequest;
import com.andrii.vaultnote.support.AbstractBaseIntegrationTest;
import com.andrii.vaultnote.users.domain.UserRole;
import com.andrii.vaultnote.users.infrastructure.persistence.entity.UserEntity;
import com.andrii.vaultnote.users.infrastructure.persistence.repository.UserJpaRepository;
import io.restassured.response.Response;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.EnumSet;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
class CsrfCorsIntegrationTest extends AbstractBaseIntegrationTest {

  private static final String CSRF_ENDPOINT = "/csrf";
  private static final String LOGIN_ENDPOINT = "/api/v1/auth/login";
  private static final String FRONTEND_ORIGIN = "http://localhost:4200";
  private static final String UNKNOWN_ORIGIN = "https://evil.example";
  private static final String USER_EMAIL = "csrf-login@example.com";
  private static final String USER_PASSWORD = "Password1234";

  UserJpaRepository userRepository;
  PasswordEncoder passwordEncoder;

  @Autowired
  CsrfCorsIntegrationTest(UserJpaRepository userRepository, PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
  }

  /**
   * Issues an Angular-compatible CSRF token and exposes its cookie/header names.
   */
  @Test
  void shouldIssueCsrfTokenForSpaClient() {
    var response = given()
        .port(port)
        .when()
        .get(CSRF_ENDPOINT)
        .then()
        .statusCode(HttpStatus.OK.value())
        .extract()
        .response();

    var token = response.jsonPath().getString("token");
    assertThat(token).isNotBlank();
    assertThat(response.jsonPath().getString("headerName")).isEqualTo("X-XSRF-TOKEN");
    assertThat(response.jsonPath().getString("parameterName")).isEqualTo("_csrf");
    assertThat(response.getCookie("XSRF-TOKEN")).isEqualTo(token);
    assertThat(response.getHeader("Set-Cookie"))
        .contains("XSRF-TOKEN=" + token)
        .contains("Path=/")
        .contains("SameSite=Lax")
        .doesNotContain("HttpOnly");
  }

  /**
   * Rejects a protected authentication request that has no CSRF token.
   */
  @Test
  void shouldRejectLoginWithoutCsrfToken() {
    userRepository.saveAndFlush(UserEntity.builder()
        .email(USER_EMAIL)
        .displayName("CSRF User")
        .passwordHash(passwordEncoder.encode(USER_PASSWORD))
        .emailVerified(true)
        .roles(EnumSet.of(UserRole.USER))
        .build());

    var response = given()
        .port(port)
        .contentType("application/json")
        .body(LoginRequest.builder()
            .email(USER_EMAIL)
            .password(USER_PASSWORD)
            .build())
        .when()
        .post(LOGIN_ENDPOINT)
        .then()
        .extract()
        .response();

    assertThat(response.statusCode())
        .as("response body: %s; headers: %s", response.asString(), response.headers())
        .isEqualTo(HttpStatus.FORBIDDEN.value());
  }

  /**
   * Allows CORS preflight requests from the configured Angular origin.
   */
  @Test
  void shouldAllowConfiguredCorsOrigin() {
    var response = corsPreflight(FRONTEND_ORIGIN);

    assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(response.getHeader("Access-Control-Allow-Origin"))
        .isEqualTo(FRONTEND_ORIGIN);
    assertThat(response.getHeader("Access-Control-Allow-Credentials"))
        .isEqualTo("true");
    assertThat(response.getHeader("Access-Control-Allow-Methods"))
        .contains("POST");
    assertThat(response.getHeader("Access-Control-Allow-Headers"))
        .containsIgnoringCase("X-XSRF-TOKEN");
  }

  /**
   * Rejects CORS preflight requests from an origin outside the allowlist.
   */
  @Test
  void shouldRejectUnknownCorsOrigin() {
    var response = corsPreflight(UNKNOWN_ORIGIN);

    assertThat(response.statusCode()).isEqualTo(HttpStatus.FORBIDDEN.value());
    assertThat(response.getHeader("Access-Control-Allow-Origin")).isNull();
  }

  private Response corsPreflight(String origin) {
    return given()
        .port(port)
        .header("Origin", origin)
        .header("Access-Control-Request-Method", "POST")
        .header("Access-Control-Request-Headers", "content-type,x-xsrf-token")
        .when()
        .options(LOGIN_ENDPOINT)
        .then()
        .extract()
        .response();
  }
}
