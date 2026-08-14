package com.andrii.vaultnote.app.api.auth;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import com.andrii.vaultnote.app.api.auth.dto.RegisterUserRequest;
import com.andrii.vaultnote.app.api.error.ApiErrorResponse;
import com.andrii.vaultnote.app.mail.MailMessage;
import com.andrii.vaultnote.app.mail.MailSender;
import com.andrii.vaultnote.support.AbstractBaseIntegrationTest;
import com.andrii.vaultnote.users.infrastructure.persistence.repository.EmailVerificationTokenJpaRepository;
import com.andrii.vaultnote.users.infrastructure.persistence.repository.UserJpaRepository;
import com.github.database.rider.core.api.dataset.DataSet;
import io.restassured.http.ContentType;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;

@DataSet(value = "auth-baseline.yml", skipCleaningFor = {"databasechangelog", "databasechangeloglock"})
@TestPropertySource(
  properties = {
    "app.security.rate-limit.registration.ip-limit=100",
    "app.security.rate-limit.registration.email-limit=2",
    "app.security.rate-limit.registration.window=PT1M"
  })
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
class RegistrationRateLimitIntegrationTest extends AbstractBaseIntegrationTest {

  private static final String REGISTRATION_ENDPOINT = "/api/v1/auth/registrations";
  private static final String EMAIL = "registration-rate-limit@example.com";
  private static final String PASSWORD = "Password1234";

  UserJpaRepository userRepository;
  EmailVerificationTokenJpaRepository tokenRepository;
  MailSender mailSender;

  @Autowired
  RegistrationRateLimitIntegrationTest(
    UserJpaRepository userRepository,
    EmailVerificationTokenJpaRepository tokenRepository,
    MailSender mailSender) {
    this.userRepository = userRepository;
    this.tokenRepository = tokenRepository;
    this.mailSender = mailSender;
  }

  @Test
  void shouldRejectRegistrationAfterEmailLimitBeforeCreatingAnotherUser() {
    var request = RegisterUserRequest.builder()
      .email(EMAIL)
      .displayName("Rate Limit User")
      .password(PASSWORD)
      .build();

    given()
      .port(port)
      .contentType(ContentType.JSON)
      .body(request)
      .when()
      .post(REGISTRATION_ENDPOINT)
      .then()
      .statusCode(HttpStatus.CREATED.value());

    given()
      .port(port)
      .contentType(ContentType.JSON)
      .body(request)
      .when()
      .post(REGISTRATION_ENDPOINT)
      .then()
      .statusCode(HttpStatus.CONFLICT.value());

    var response = given()
      .port(port)
      .contentType(ContentType.JSON)
      .body(request)
      .when()
      .post(REGISTRATION_ENDPOINT)
      .then()
      .statusCode(HttpStatus.TOO_MANY_REQUESTS.value())
      .extract()
      .response();

    var errorResponse = response.as(ApiErrorResponse.class);
    var retryAfter = Long.parseLong(response.getHeader("Retry-After"));

    assertThat(errorResponse.code()).isEqualTo("RATE_LIMIT_EXCEEDED");
    assertThat(retryAfter).isBetween(1L, 60L);
    assertThat(userRepository.findByEmail(EMAIL)).isPresent();
    assertThat(tokenRepository.findAll()).hasSize(1);
    verify(mailSender).send(any(MailMessage.class));
  }
}
