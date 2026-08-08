package com.andrii.vaultnote.app.api.auth;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import com.andrii.vaultnote.app.api.auth.dto.RegisterUserRequest;
import com.andrii.vaultnote.app.api.auth.dto.RegisterUserResponse;
import com.andrii.vaultnote.app.api.error.ApiErrorResponse;
import com.andrii.vaultnote.support.AbstractBaseIntegrationTest;
import com.andrii.vaultnote.users.infrastructure.persistence.UserJpaRepository;
import com.github.database.rider.core.api.dataset.DataSet;
import io.restassured.http.ContentType;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

@DataSet(value = "datasets/users.yml", cleanBefore = true, cleanAfter = true, skipCleaningFor = {"databasechangelog",
    "databasechangeloglock"})
class RegistrationIntegrationTest extends AbstractBaseIntegrationTest {

  private final UserJpaRepository userJpaRepository;

  @Autowired
  RegistrationIntegrationTest(UserJpaRepository userJpaRepository) {
    this.userJpaRepository = userJpaRepository;
  }

  @Test
  void shouldRegisterUserAgainstPostgres() {
    var email = uniqueEmail();
    var request = RegisterUserRequest.builder()
        .email(email)
        .displayName("Integration User")
        .password("password")
        .build();
    var requestStartedAt = Instant.now();

    var response = given()
        .port(port)
        .contentType(ContentType.JSON)
        .body(request)
        .when()
        .post("/api/v1/auth/registrations")
        .then()
        .statusCode(HttpStatus.CREATED.value())
        .extract()
        .as(RegisterUserResponse.class);

    var requestFinishedAt = Instant.now();

    assertThat(response.userId()).isPositive();

    var savedUser = userJpaRepository.findByEmail(email).orElseThrow();
    assertThat(savedUser.getDisplayName()).isEqualTo("Integration User");
    assertThat(savedUser.getPasswordHash())
        .isNotBlank()
        .isNotEqualTo("password");
    assertThat(savedUser.isEmailVerified()).isFalse();
    assertThat(savedUser.getCreatedAt()).isBetween(requestStartedAt, requestFinishedAt);
    assertThat(savedUser.getUpdatedAt()).isBetween(requestStartedAt, requestFinishedAt);
    assertThat(savedUser.getUpdatedAt()).isAfterOrEqualTo(savedUser.getCreatedAt());
  }

  @Test
  void shouldRejectDuplicateEmail() {
    var request = RegisterUserRequest.builder()
        .email("existing@example.com")
        .displayName("New User")
        .password("password")
        .build();

    var response = given()
        .port(port)
        .contentType(ContentType.JSON)
        .body(request)
        .when()
        .post("/api/v1/auth/registrations")
        .then()
        .statusCode(HttpStatus.CONFLICT.value())
        .extract()
        .as(ApiErrorResponse.class);

    assertThat(response.code()).isEqualTo("ENTITY_ALREADY_EXISTS");
  }

  private static String uniqueEmail() {
    return "integration-" + UUID.randomUUID() + "@example.com";
  }
}
