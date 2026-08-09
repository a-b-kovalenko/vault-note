package com.andrii.vaultnote.app.api.auth;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.andrii.vaultnote.app.api.auth.dto.RegisterUserRequest;
import com.andrii.vaultnote.app.api.auth.dto.RegisterUserResponse;
import com.andrii.vaultnote.app.api.error.ApiErrorResponse;
import com.andrii.vaultnote.app.api.error.ValidationViolation;
import com.andrii.vaultnote.app.mail.MailMessage;
import com.andrii.vaultnote.app.mail.MailSender;
import com.andrii.vaultnote.users.domain.UserRole;
import com.andrii.vaultnote.users.infrastructure.persistence.repository.EmailVerificationTokenJpaRepository;
import com.andrii.vaultnote.support.AbstractBaseIntegrationTest;
import com.andrii.vaultnote.users.infrastructure.persistence.repository.UserJpaRepository;
import com.github.database.rider.core.api.dataset.DataSet;
import io.restassured.http.ContentType;
import java.time.Instant;
import java.util.UUID;
import java.util.stream.Stream;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

@DataSet(value = "auth-baseline.yml", skipCleaningFor = {"databasechangelog", "databasechangeloglock"})
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
class RegistrationIntegrationTest extends AbstractBaseIntegrationTest {

  private static final String PASSWORD = "Password1234";

  UserJpaRepository userJpaRepository;
  EmailVerificationTokenJpaRepository tokenRepository;
  MailSender mailSender;

  @Autowired
  RegistrationIntegrationTest(
      UserJpaRepository userJpaRepository,
      EmailVerificationTokenJpaRepository tokenRepository,
      MailSender mailSender) {
    this.userJpaRepository = userJpaRepository;
    this.tokenRepository = tokenRepository;
    this.mailSender = mailSender;
  }

  /**
   * Registers a new user against PostgreSQL.
   *
   * <p>
   * The endpoint must return {@code 201 Created}, persist a hashed password and
   * an unverified user, create an email verification token, and send the
   * verification email.
   */
  @Test
  void shouldRegisterUserAgainstPostgres() {
    var email = uniqueEmail();
    var request = RegisterUserRequest.builder()
        .email(email)
        .displayName("Integration User")
        .password(PASSWORD)
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
        .isNotEqualTo(PASSWORD)
        .startsWith("$argon2id$");
    assertThat(savedUser.isEmailVerified()).isFalse();
    assertThat(savedUser.getRoles()).containsExactly(UserRole.USER);
    assertThat(savedUser.getCreatedAt()).isBetween(requestStartedAt, requestFinishedAt);
    assertThat(savedUser.getUpdatedAt()).isBetween(requestStartedAt, requestFinishedAt);
    assertThat(savedUser.getUpdatedAt()).isAfterOrEqualTo(savedUser.getCreatedAt());

    assertThat(tokenRepository.findAll())
        .singleElement()
        .satisfies(token -> {
          assertThat(token.getUser().getId()).isEqualTo(savedUser.getId());
          assertThat(token.getTokenHash()).hasSize(64);
          assertThat(token.getExpiresAt()).isAfter(requestFinishedAt);
          assertThat(token.getUsedAt()).isNull();
        });

    var mailCaptor = ArgumentCaptor.forClass(MailMessage.class);
    verify(mailSender).send(mailCaptor.capture());
    assertThat(mailCaptor.getValue().to()).isEqualTo(email);
    assertThat(mailCaptor.getValue().text())
        .startsWith("Please verify your VaultNote email by opening this link:")
        .contains("http://localhost:4200/verify-email?token=");
  }

  /**
   * Rejects registration when the requested email is already registered.
   *
   * <p>
   * The endpoint must return {@code 409 Conflict} with
   * {@code ENTITY_ALREADY_EXISTS}.
   */
  @Test
  void shouldRejectDuplicateEmail() {
    var request = RegisterUserRequest.builder()
        .email("existing@example.com")
        .displayName("New User")
        .password(PASSWORD)
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

  /**
   * Rejects registration requests that violate field validation or the password
   * policy.
   *
   * @param scenario
   *          name of the invalid input scenario
   * @param request
   *          registration request under test
   * @param expectedField
   *          expected field reported in the validation violation
   * @param expectedCode
   *          expected API validation code
   */
  @ParameterizedTest(name = "{0}")
  @MethodSource("invalidRegistrationRequests")
  void shouldRejectInvalidRegistrationRequest(
      String scenario,
      RegisterUserRequest request,
      String expectedField,
      String expectedCode) {

    var response = given()
        .port(port)
        .contentType(ContentType.JSON)
        .body(request)
        .when()
        .post("/api/v1/auth/registrations")
        .then()
        .statusCode(HttpStatus.BAD_REQUEST.value())
        .extract()
        .as(ApiErrorResponse.class);

    assertThat(response.code()).isEqualTo("VALIDATION_FAILED");
    assertThat(response.violations())
        .singleElement()
        .extracting(ValidationViolation::field, ValidationViolation::code)
        .containsExactly(expectedField, expectedCode);
  }

  private static Stream<Arguments> invalidRegistrationRequests() {
    var validRequest = RegisterUserRequest.builder()
        .email("validation-" + UUID.randomUUID() + "@example.com")
        .displayName("Valid User")
        .password(PASSWORD)
        .build();

    return Stream.of(
        Arguments.of(
            "blank email",
            validRequest.toBuilder().email("").build(),
            "email",
            "REQUIRED"),
        Arguments.of(
            "malformed email",
            validRequest.toBuilder().email("invalid-email").build(),
            "email",
            "INVALID_FORMAT"),
        Arguments.of(
            "blank display name",
            validRequest.toBuilder().displayName(" ").build(),
            "display_name",
            "REQUIRED"),
        Arguments.of(
            "display name too long",
            validRequest.toBuilder().displayName("a".repeat(101)).build(),
            "display_name",
            "INVALID_LENGTH"),
        Arguments.of(
            "password too short",
            validRequest.toBuilder().password("password12").build(),
            "password",
            "INVALID_LENGTH"),
        Arguments.of(
            "password without alphabetic character",
            validRequest.toBuilder().password("12345678901112").build(),
            "password",
            "PASSWORD_POLICY"),
        Arguments.of(
            "password with one digit",
            validRequest.toBuilder().password("onlyOneDigit1").build(),
            "password",
            "PASSWORD_POLICY"));
  }

  private static String uniqueEmail() {
    return "integration-" + UUID.randomUUID() + "@example.com";
  }
}
