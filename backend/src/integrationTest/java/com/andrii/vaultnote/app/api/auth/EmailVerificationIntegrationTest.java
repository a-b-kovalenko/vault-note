package com.andrii.vaultnote.app.api.auth;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import com.andrii.vaultnote.app.api.error.ApiErrorResponse;
import com.andrii.vaultnote.app.security.SecureTokenGenerator;
import com.andrii.vaultnote.app.security.SecureTokenGenerator.GeneratedToken;
import com.andrii.vaultnote.users.infrastructure.persistence.entity.EmailVerificationTokenEntity;
import com.andrii.vaultnote.users.infrastructure.persistence.repository.EmailVerificationTokenJpaRepository;
import com.andrii.vaultnote.users.infrastructure.persistence.repository.UserJpaRepository;
import com.andrii.vaultnote.support.AbstractBaseIntegrationTest;
import com.github.database.rider.core.api.dataset.DataSet;
import io.restassured.response.Response;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.context.annotation.Primary;

@DataSet(value = "auth-baseline.yml", skipCleaningFor = {"databasechangelog", "databasechangeloglock"})
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Import(EmailVerificationIntegrationTest.TestClockConfiguration.class)
class EmailVerificationIntegrationTest extends AbstractBaseIntegrationTest {

  private static final String EMAIL_VERIFICATION_ENDPOINT = "/api/v1/auth/email-verification";
  private static final Instant FIXED_NOW = Instant.parse("2099-01-01T00:00:00Z");
  private static final String USER_EMAIL = "verification@example.com";

  UserJpaRepository userRepository;
  EmailVerificationTokenJpaRepository tokenRepository;
  SecureTokenGenerator tokenGenerator;
  Clock clock;

  @Autowired
  EmailVerificationIntegrationTest(
    UserJpaRepository userRepository,
    EmailVerificationTokenJpaRepository tokenRepository,
    SecureTokenGenerator tokenGenerator,
    Clock clock) {
    this.userRepository = userRepository;
    this.tokenRepository = tokenRepository;
    this.tokenGenerator = tokenGenerator;
    this.clock = clock;
  }

  /**
   * Verifies an email using a valid, unexpired token.
   *
   * <p>
   * The endpoint must return {@code 204 No Content}, mark the user as verified,
   * and mark the token as used.
   */
  @Test
  void shouldVerifyEmailAgainstPostgres() {
    var user = userRepository.findByEmail(USER_EMAIL).orElseThrow();
    GeneratedToken generatedToken = tokenGenerator.generate();
    var token = EmailVerificationTokenEntity.builder()
      .user(user)
      .tokenHash(generatedToken.hash())
      .expiresAt(clock.instant().plus(Duration.ofHours(1)))
      .build();
    tokenRepository.saveAndFlush(token);

    var response = given()
      .port(port)
      .queryParam("token", generatedToken.rawValue())
      .when()
      .post(EMAIL_VERIFICATION_ENDPOINT)
      .then()
      .statusCode(HttpStatus.NO_CONTENT.value())
      .extract()
      .response();

    assertThat(response.asByteArray()).isEmpty();

    var verifiedUser = userRepository.findByEmail(USER_EMAIL).orElseThrow();
    assertThat(verifiedUser.isEmailVerified()).isTrue();

    assertThat(tokenRepository.findAll())
      .singleElement()
      .extracting(EmailVerificationTokenEntity::getUsedAt)
      .isNotNull();
  }

  /**
   * Rejects a token that does not exist in the database.
   *
   * <p>
   * The endpoint must return {@code 400 Bad Request} with
   * {@code EMAIL_VERIFICATION_FAILED}, without changing the user or token tables.
   */
  @Test
  void shouldRejectInvalidTokenAgainstPostgres() {
    var invalidToken = UUID.randomUUID().toString();

    var response = given()
      .port(port)
      .queryParam("token", invalidToken)
      .when()
      .post(EMAIL_VERIFICATION_ENDPOINT)
      .then()
      .statusCode(HttpStatus.BAD_REQUEST.value())
      .extract()
      .as(ApiErrorResponse.class);

    assertThat(response.code()).isEqualTo("EMAIL_VERIFICATION_FAILED");

    var user = userRepository.findByEmail(USER_EMAIL).orElseThrow();
    assertThat(user.isEmailVerified()).isFalse();
    assertThat(tokenRepository.findAll()).isEmpty();
  }

  /**
   * Rejects a token whose expiration time is in the past.
   *
   * <p>
   * The endpoint must return {@code 400 Bad Request}, leave the user unverified,
   * and keep the token unused.
   */
  @Test
  void shouldRejectExpiredTokenAgainstPostgres() {
    var user = userRepository.findByEmail(USER_EMAIL).orElseThrow();
    GeneratedToken generatedToken = tokenGenerator.generate();
    var token = EmailVerificationTokenEntity.builder()
      .user(user)
      .tokenHash(generatedToken.hash())
      .expiresAt(clock.instant().minus(Duration.ofHours(1)))
      .build();
    tokenRepository.saveAndFlush(token);

    var response = given()
      .port(port)
      .queryParam("token", generatedToken.rawValue())
      .when()
      .post(EMAIL_VERIFICATION_ENDPOINT)
      .then()
      .statusCode(HttpStatus.BAD_REQUEST.value())
      .extract()
      .as(ApiErrorResponse.class);

    assertThat(response.code()).isEqualTo("EMAIL_VERIFICATION_FAILED");

    var unchangedUser = userRepository.findByEmail(USER_EMAIL).orElseThrow();
    assertThat(unchangedUser.isEmailVerified()).isFalse();
    assertThat(tokenRepository.findAll())
      .singleElement()
      .extracting(EmailVerificationTokenEntity::getUsedAt)
      .isNull();
  }

  /**
   * Rejects a token after it has already been used successfully.
   *
   * <p>
   * The first request must return {@code 204 No Content}. A repeated request with
   * the same token must return {@code 400 Bad Request}, while the user remains
   * verified and the token's {@code used_at} value remains unchanged.
   */
  @Test
  void shouldRejectReusedTokenAgainstPostgres() {
    var user = userRepository.findByEmail(USER_EMAIL).orElseThrow();
    GeneratedToken generatedToken = tokenGenerator.generate();
    var token = EmailVerificationTokenEntity.builder()
      .user(user)
      .tokenHash(generatedToken.hash())
      .expiresAt(clock.instant().plus(Duration.ofHours(1)))
      .build();
    tokenRepository.saveAndFlush(token);

    given()
      .port(port)
      .queryParam("token", generatedToken.rawValue())
      .when()
      .post(EMAIL_VERIFICATION_ENDPOINT)
      .then()
      .statusCode(HttpStatus.NO_CONTENT.value());

    var tokenAfterFirstUse = tokenRepository.findAll().stream().findFirst().orElseThrow();
    var usedAtAfterFirstUse = tokenAfterFirstUse.getUsedAt();
    assertThat(usedAtAfterFirstUse).isNotNull();

    var response = given()
      .port(port)
      .queryParam("token", generatedToken.rawValue())
      .when()
      .post(EMAIL_VERIFICATION_ENDPOINT)
      .then()
      .statusCode(HttpStatus.BAD_REQUEST.value())
      .extract()
      .as(ApiErrorResponse.class);

    assertThat(response.code()).isEqualTo("EMAIL_VERIFICATION_FAILED");

    var verifiedUser = userRepository.findByEmail(USER_EMAIL).orElseThrow();
    assertThat(verifiedUser.isEmailVerified()).isTrue();
    assertThat(tokenRepository.findAll())
      .singleElement()
      .extracting(EmailVerificationTokenEntity::getUsedAt)
      .isEqualTo(usedAtAfterFirstUse);
  }

  /**
   * Allows two concurrent requests to use the same valid token.
   *
   * <p>
   * Exactly one request must return {@code 204 No Content}; the other must return
   * {@code 400 Bad Request}. The user must be verified and the token must have
   * one used timestamp.
   */
  @Test
  void shouldAllowOnlyOneConcurrentTokenUseAgainstPostgres() {
    var user = userRepository.findByEmail(USER_EMAIL).orElseThrow();
    GeneratedToken generatedToken = tokenGenerator.generate();
    var token = EmailVerificationTokenEntity.builder()
      .user(user)
      .tokenHash(generatedToken.hash())
      .expiresAt(clock.instant().plus(Duration.ofHours(1)))
      .build();
    tokenRepository.saveAndFlush(token);

    var ready = new CountDownLatch(2);
    var start = new CountDownLatch(1);
    try (var executor = Executors.newFixedThreadPool(2)) {
      var futures = IntStream.range(0, 2)
        .mapToObj(ignored -> executor.submit(() -> {
          ready.countDown();
          start.await();
          return sendVerificationRequest(generatedToken.rawValue());
        }))
        .toList();

      try {
        assertThat(awaitReady(ready)).isTrue();
      } finally {
        start.countDown();
      }

      var responses = futures.stream()
        .map(EmailVerificationIntegrationTest::awaitResponse)
        .toList();

      assertThat(responses)
        .extracting(Response::statusCode)
        .containsExactlyInAnyOrder(
          HttpStatus.NO_CONTENT.value(),
          HttpStatus.BAD_REQUEST.value());

      var failedResponse = responses.stream()
        .filter(response -> response.statusCode() == HttpStatus.BAD_REQUEST.value())
        .findFirst()
        .orElseThrow()
        .as(ApiErrorResponse.class);
      assertThat(failedResponse.code()).isEqualTo("EMAIL_VERIFICATION_FAILED");
    }

    var verifiedUser = userRepository.findByEmail(USER_EMAIL).orElseThrow();
    assertThat(verifiedUser.isEmailVerified()).isTrue();
    assertThat(tokenRepository.findAll())
      .singleElement()
      .extracting(EmailVerificationTokenEntity::getUsedAt)
      .isNotNull();
  }

  private Response sendVerificationRequest(String rawToken) {
    return given()
      .port(port)
      .queryParam("token", rawToken)
      .when()
      .post(EMAIL_VERIFICATION_ENDPOINT)
      .then()
      .extract()
      .response();
  }

  private static Response awaitResponse(Future<Response> future) {
    try {
      return future.get(10, TimeUnit.SECONDS);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new AssertionError("Interrupted while awaiting verification response", exception);
    } catch (ExecutionException | TimeoutException exception) {
      throw new AssertionError("Could not obtain verification response", exception);
    }
  }

  private static boolean awaitReady(CountDownLatch ready) {
    try {
      return ready.await(10, TimeUnit.SECONDS);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new AssertionError("Interrupted while awaiting concurrent requests", exception);
    }
  }

  @TestConfiguration(proxyBeanMethods = false)
  static class TestClockConfiguration {

    @Bean
    @Primary
    Clock testClock() {
      return Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
    }
  }
}
