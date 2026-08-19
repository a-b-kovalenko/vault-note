package com.andrii.vaultnote.app.api.security;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import com.andrii.vaultnote.app.api.auth.dto.LoginRequest;
import com.andrii.vaultnote.support.AbstractBaseIntegrationTest;
import com.andrii.vaultnote.users.domain.UserRole;
import com.andrii.vaultnote.users.infrastructure.persistence.entity.UserEntity;
import com.andrii.vaultnote.users.infrastructure.persistence.repository.UserJpaRepository;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.util.EnumSet;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
class StatelessCsrfIntegrationTest extends AbstractBaseIntegrationTest {

  private static final String CSRF_ENDPOINT = "/csrf";
  private static final String LOGIN_ENDPOINT = "/api/v1/auth/login";
  private static final String CSRF_HEADER_NAME = "X-XSRF-TOKEN";
  private static final String CSRF_COOKIE_NAME = "XSRF-TOKEN";
  private static final String USER_EMAIL = "stateless-csrf-login@example.com";
  private static final String USER_PASSWORD = "Password1234";

  UserJpaRepository userRepository;
  PasswordEncoder passwordEncoder;

  @Autowired
  StatelessCsrfIntegrationTest(
    UserJpaRepository userRepository,
    PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @Test
  void shouldIssueTokenWithoutXsrfCookie() {
    var response = getCsrfToken();

    assertThat(response.jsonPath().getString("token")).isNotBlank();
    assertThat(response.jsonPath().getString("headerName")).isEqualTo(CSRF_HEADER_NAME);
    assertThat(response.jsonPath().getString("parameterName")).isEqualTo("_csrf");
    assertThat(response.getCookie(CSRF_COOKIE_NAME)).isNull();
    assertThat(response.getHeader("Set-Cookie")).isNull();
  }

  @Test
  void shouldAcceptProtectedRequestWithValidHeaderWithoutCookie() {
    userRepository.saveAndFlush(UserEntity.builder()
      .email(USER_EMAIL)
      .displayName("Stateless CSRF User")
      .passwordHash(passwordEncoder.encode(USER_PASSWORD))
      .emailVerified(true)
      .roles(EnumSet.of(UserRole.USER))
      .build());
    var csrfResponse = getCsrfToken();
    var token = csrfResponse.jsonPath().getString("token");

    var response = given()
      .port(port)
      .contentType(ContentType.JSON)
      .header(CSRF_HEADER_NAME, token)
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
      .isEqualTo(HttpStatus.OK.value());
  }

  @Test
  void shouldRejectProtectedRequestWithInvalidHeader() {
    var response = given()
      .port(port)
      .contentType(ContentType.JSON)
      .header(CSRF_HEADER_NAME, "invalid-token")
      .body(LoginRequest.builder()
        .email(USER_EMAIL)
        .password(USER_PASSWORD)
        .build())
      .when()
      .post(LOGIN_ENDPOINT)
      .then()
      .extract()
      .response();

    assertThat(response.statusCode()).isEqualTo(HttpStatus.FORBIDDEN.value());
  }

  private Response getCsrfToken() {
    return given()
      .port(port)
      .when()
      .get(CSRF_ENDPOINT)
      .then()
      .statusCode(HttpStatus.OK.value())
      .extract()
      .response();
  }
}
