package com.andrii.vaultnote.app.api.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.andrii.vaultnote.app.api.auth.dto.LoginRequest;
import com.andrii.vaultnote.app.api.auth.dto.PasswordResetConfirmRequest;
import com.andrii.vaultnote.app.api.auth.dto.PasswordResetRequest;
import com.andrii.vaultnote.app.api.error.ApiErrorResponse;
import com.andrii.vaultnote.app.mail.MailMessage;
import com.andrii.vaultnote.app.mail.MailSender;
import com.andrii.vaultnote.app.security.SecureTokenGenerator;
import com.andrii.vaultnote.app.security.SecureTokenGenerator.GeneratedToken;
import com.andrii.vaultnote.support.AbstractBaseIntegrationTest;
import com.andrii.vaultnote.users.domain.UserRole;
import com.andrii.vaultnote.users.infrastructure.persistence.entity.PasswordResetTokenEntity;
import com.andrii.vaultnote.users.infrastructure.persistence.entity.UserEntity;
import com.andrii.vaultnote.users.infrastructure.persistence.repository.PasswordResetTokenJpaRepository;
import com.andrii.vaultnote.users.infrastructure.persistence.repository.RefreshTokenJpaRepository;
import com.andrii.vaultnote.users.infrastructure.persistence.repository.UserJpaRepository;
import com.github.database.rider.core.api.dataset.DataSet;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.IntStream;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.util.UriComponentsBuilder;

@DataSet(value = "auth-baseline.yml", skipCleaningFor = {"databasechangelog", "databasechangeloglock"})
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Import(PasswordResetIntegrationTest.TestClockConfiguration.class)
class PasswordResetIntegrationTest extends AbstractBaseIntegrationTest {

  private static final String PASSWORD_RESET_REQUEST_ENDPOINT = "/api/v1/auth/password-reset/request";
  private static final String PASSWORD_RESET_CONFIRM_ENDPOINT = "/api/v1/auth/password-reset/confirm";
  private static final String PASSWORD_RESET_FAILED_CODE = "PASSWORD_RESET_FAILED";
  private static final String LOGIN_ENDPOINT = "/api/v1/auth/login";
  private static final String REFRESH_COOKIE_NAME = "vaultnote_refresh_token";
  private static final String PASSWORD = "Password1234";
  private static final String NEW_PASSWORD = "NewPassword1234";
  private static final Instant FIXED_NOW = Instant.parse("2099-01-01T00:00:00Z");
  private static final Duration TOKEN_TTL = Duration.ofHours(1);

  UserJpaRepository userRepository;
  PasswordResetTokenJpaRepository passwordResetTokenRepository;
  RefreshTokenJpaRepository refreshTokenRepository;
  SecureTokenGenerator tokenGenerator;
  PasswordEncoder passwordEncoder;
  MailSender mailSender;

  @Autowired
  PasswordResetIntegrationTest(
    UserJpaRepository userRepository,
    PasswordResetTokenJpaRepository passwordResetTokenRepository,
    RefreshTokenJpaRepository refreshTokenRepository,
    SecureTokenGenerator tokenGenerator,
    PasswordEncoder passwordEncoder,
    MailSender mailSender) {
    this.userRepository = userRepository;
    this.passwordResetTokenRepository = passwordResetTokenRepository;
    this.refreshTokenRepository = refreshTokenRepository;
    this.tokenGenerator = tokenGenerator;
    this.passwordEncoder = passwordEncoder;
    this.mailSender = mailSender;
  }

  /**
   * Accepts a reset request for an unknown email without creating observable
   * reset state.
   *
   * <p>
   * The endpoint must return the same neutral {@code 202 Accepted} used for an
   * existing account and must not send an email or create a reset token.
   */
  @Test
  void shouldAcceptUnknownEmailWithoutCreatingResetState() {
    clearInvocations(mailSender);
    var request = PasswordResetRequest.builder()
      .email(uniqueEmail())
      .build();

    var response = givenWithCsrf()
      .port(port)
      .contentType(ContentType.JSON)
      .body(request)
      .when()
      .post(PASSWORD_RESET_REQUEST_ENDPOINT)
      .then()
      .statusCode(HttpStatus.ACCEPTED.value())
      .extract()
      .response();

    assertThat(response.asByteArray()).isEmpty();
    assertThat(passwordResetTokenRepository.findAll()).isEmpty();
    verifyNoInteractions(mailSender);
  }

  /**
   * Issues a hashed, expiring reset token for an existing account.
   *
   * <p>
   * The raw token must appear only in the personalized email link, while the
   * database stores its hash and the endpoint returns a neutral response.
   */
  @Test
  void shouldIssuePasswordResetTokenAndEmailAgainstPostgres() {
    var email = uniqueEmail();
    var user = saveUser(email, false);
    clearInvocations(mailSender);
    var request = PasswordResetRequest.builder()
      .email(email)
      .build();

    var response = givenWithCsrf()
      .port(port)
      .contentType(ContentType.JSON)
      .body(request)
      .when()
      .post(PASSWORD_RESET_REQUEST_ENDPOINT)
      .then()
      .statusCode(HttpStatus.ACCEPTED.value())
      .extract()
      .response();

    assertThat(response.asByteArray()).isEmpty();
    var resetToken = passwordResetTokenRepository.findAll()
      .stream()
      .findFirst()
      .orElseThrow();
    var mailMessage = captureMailMessage();
    var rawToken = extractResetToken(mailMessage);

    assertThat(resetToken.getUser().getId()).isEqualTo(user.getId());
    assertThat(resetToken.getTokenHash())
      .hasSize(64)
      .isEqualTo(tokenGenerator.hash(rawToken))
      .isNotEqualTo(rawToken);
    assertThat(resetToken.getExpiresAt()).isEqualTo(FIXED_NOW.plus(TOKEN_TTL));
    assertThat(resetToken.getUsedAt()).isNull();
    assertThat(resetToken.getInvalidatedAt()).isNull();
    assertThat(mailMessage)
      .extracting(MailMessage::to, MailMessage::subject)
      .containsExactly(email, "Reset your VaultNote password");
    assertThat(mailMessage.text())
      .startsWith("Hello " + user.getDisplayName() + ",\n\n")
      .contains("http://localhost:4200/reset-password?token=" + rawToken);
  }

  /**
   * Invalidates the previous active token when a new reset request is issued.
   */
  @Test
  void shouldInvalidatePreviousResetTokenAgainstPostgres() {
    var email = uniqueEmail();
    saveUser(email, false);

    requestPasswordReset(email);
    var firstRawToken = extractResetToken(captureMailMessage());
    clearInvocations(mailSender);

    requestPasswordReset(email);
    var secondRawToken = extractResetToken(captureMailMessage());

    var persistedTokens = passwordResetTokenRepository.findAll();
    var firstToken = persistedTokens.stream()
      .filter(token -> token.getTokenHash().equals(tokenGenerator.hash(firstRawToken)))
      .findFirst()
      .orElseThrow();
    var secondToken = persistedTokens.stream()
      .filter(token -> token.getTokenHash().equals(tokenGenerator.hash(secondRawToken)))
      .findFirst()
      .orElseThrow();

    assertThat(persistedTokens).hasSize(2);
    assertThat(firstToken.getInvalidatedAt()).isEqualTo(FIXED_NOW);
    assertThat(secondToken.getInvalidatedAt()).isNull();
  }

  /**
   * Rejects an expired reset token without changing the account or token state.
   */
  @Test
  void shouldRejectExpiredResetTokenAgainstPostgres() {
    var user = saveUser(uniqueEmail(), false);
    var generatedToken = saveResetToken(user, FIXED_NOW.minus(Duration.ofHours(1)));
    var request = resetConfirmation(generatedToken.rawValue());

    var response = givenWithCsrf()
      .port(port)
      .contentType(ContentType.JSON)
      .body(request)
      .when()
      .post(PASSWORD_RESET_CONFIRM_ENDPOINT)
      .then()
      .statusCode(HttpStatus.BAD_REQUEST.value())
      .extract()
      .as(ApiErrorResponse.class);

    assertThat(response.code()).isEqualTo(PASSWORD_RESET_FAILED_CODE);
    var unchangedUser = userRepository.findById(user.getId()).orElseThrow();
    var unchangedToken = passwordResetTokenRepository.findAll()
      .stream()
      .findFirst()
      .orElseThrow();
    assertThat(unchangedUser.isEmailVerified()).isFalse();
    assertThat(unchangedUser.getPasswordHash()).isEqualTo(user.getPasswordHash());
    assertThat(unchangedToken.getUsedAt()).isNull();
  }

  /**
   * Consumes a reset token once and rejects a second confirmation with the same
   * raw token.
   */
  @Test
  void shouldRejectReusedResetTokenAgainstPostgres() {
    var user = saveUser(uniqueEmail(), false);
    var generatedToken = saveResetToken(user, FIXED_NOW.plus(TOKEN_TTL));
    var request = resetConfirmation(generatedToken.rawValue());

    givenWithCsrf()
      .port(port)
      .contentType(ContentType.JSON)
      .body(request)
      .when()
      .post(PASSWORD_RESET_CONFIRM_ENDPOINT)
      .then()
      .statusCode(HttpStatus.NO_CONTENT.value());

    var usedAtAfterFirstConfirmation = passwordResetTokenRepository.findAll()
      .stream()
      .findFirst()
      .orElseThrow()
      .getUsedAt();
    var response = givenWithCsrf()
      .port(port)
      .contentType(ContentType.JSON)
      .body(request)
      .when()
      .post(PASSWORD_RESET_CONFIRM_ENDPOINT)
      .then()
      .statusCode(HttpStatus.BAD_REQUEST.value())
      .extract()
      .as(ApiErrorResponse.class);

    assertThat(response.code()).isEqualTo(PASSWORD_RESET_FAILED_CODE);
    assertThat(passwordResetTokenRepository.findAll())
      .singleElement()
      .extracting(PasswordResetTokenEntity::getUsedAt)
      .isEqualTo(usedAtAfterFirstConfirmation);
    assertThat(userRepository.findById(user.getId()).orElseThrow().getPasswordHash())
      .isNotEqualTo(user.getPasswordHash());
  }

  /**
   * Resets the password and revokes all active refresh sessions for the account.
   */
  @Test
  void shouldResetPasswordAndRevokeRefreshSessionAgainstPostgres() {
    var user = saveUser(uniqueEmail(), true);
    var loginResponse = login(user.getEmail());
    var rawRefreshToken = loginResponse.getCookie(REFRESH_COOKIE_NAME);
    var generatedToken = saveResetToken(user, FIXED_NOW.plus(TOKEN_TTL));
    var request = resetConfirmation(generatedToken.rawValue());

    var response = givenWithCsrf()
      .port(port)
      .contentType(ContentType.JSON)
      .body(request)
      .when()
      .post(PASSWORD_RESET_CONFIRM_ENDPOINT)
      .then()
      .statusCode(HttpStatus.NO_CONTENT.value())
      .extract()
      .response();

    var updatedUser = userRepository.findById(user.getId()).orElseThrow();
    var persistedRefreshToken = refreshTokenRepository.findAll()
      .stream()
      .findFirst()
      .orElseThrow();
    var persistedResetToken = passwordResetTokenRepository.findAll()
      .stream()
      .findFirst()
      .orElseThrow();

    assertThat(response.asByteArray()).isEmpty();
    assertThat(response.getHeader("Set-Cookie"))
      .contains(REFRESH_COOKIE_NAME + "=")
      .contains("Max-Age=0")
      .contains("HttpOnly");
    assertThat(rawRefreshToken).isNotBlank();
    assertThat(updatedUser.isEmailVerified()).isTrue();
    assertThat(passwordEncoder.matches(NEW_PASSWORD, updatedUser.getPasswordHash())).isTrue();
    assertThat(persistedResetToken.getUsedAt()).isEqualTo(FIXED_NOW);
    assertThat(persistedRefreshToken.getRevokedAt()).isEqualTo(FIXED_NOW);
  }

  /**
   * Allows only one concurrent request to consume a valid reset token.
   */
  @Test
  void shouldAllowOnlyOneConcurrentResetTokenUseAgainstPostgres() {
    var user = saveUser(uniqueEmail(), false);
    var generatedToken = saveResetToken(user, FIXED_NOW.plus(TOKEN_TTL));
    var ready = new CountDownLatch(2);
    var start = new CountDownLatch(1);

    try (var executor = Executors.newFixedThreadPool(2)) {
      var futures = IntStream.range(0, 2)
        .mapToObj(ignored -> executor.submit(() -> {
          ready.countDown();
          start.await();
          return sendResetConfirmation(generatedToken.rawValue());
        }))
        .toList();

      try {
        assertThat(awaitReady(ready)).isTrue();
      } finally {
        start.countDown();
      }

      var responses = futures.stream()
        .map(PasswordResetIntegrationTest::awaitResponse)
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
      assertThat(failedResponse.code()).isEqualTo(PASSWORD_RESET_FAILED_CODE);
    }

    var updatedUser = userRepository.findById(user.getId()).orElseThrow();
    assertThat(updatedUser.isEmailVerified()).isTrue();
    assertThat(passwordEncoder.matches(NEW_PASSWORD, updatedUser.getPasswordHash())).isTrue();
    assertThat(passwordResetTokenRepository.findAll())
      .singleElement()
      .extracting(PasswordResetTokenEntity::getUsedAt)
      .isEqualTo(FIXED_NOW);
  }

  private void requestPasswordReset(String email) {
    var request = PasswordResetRequest.builder()
      .email(email)
      .build();

    givenWithCsrf()
      .port(port)
      .contentType(ContentType.JSON)
      .body(request)
      .when()
      .post(PASSWORD_RESET_REQUEST_ENDPOINT)
      .then()
      .statusCode(HttpStatus.ACCEPTED.value());
  }

  private Response login(String email) {
    var request = LoginRequest.builder()
      .email(email)
      .password(PASSWORD)
      .build();

    return givenWithCsrf()
      .port(port)
      .contentType(ContentType.JSON)
      .body(request)
      .when()
      .post(LOGIN_ENDPOINT)
      .then()
      .statusCode(HttpStatus.OK.value())
      .extract()
      .response();
  }

  private UserEntity saveUser(String email, boolean emailVerified) {
    return userRepository.saveAndFlush(UserEntity.builder()
      .email(email)
      .displayName("Password Reset User")
      .passwordHash(passwordEncoder.encode(PASSWORD))
      .emailVerified(emailVerified)
      .roles(EnumSet.of(UserRole.USER))
      .build());
  }

  private GeneratedToken saveResetToken(UserEntity user, Instant expiresAt) {
    var generatedToken = tokenGenerator.generate();
    passwordResetTokenRepository.saveAndFlush(PasswordResetTokenEntity.builder()
      .user(user)
      .tokenHash(generatedToken.hash())
      .expiresAt(expiresAt)
      .build());
    return generatedToken;
  }

  private PasswordResetConfirmRequest resetConfirmation(String rawToken) {
    return PasswordResetConfirmRequest.builder()
      .token(rawToken)
      .newPassword(NEW_PASSWORD)
      .build();
  }

  private Response sendResetConfirmation(String rawToken) {
    return givenWithCsrf()
      .port(port)
      .contentType(ContentType.JSON)
      .body(resetConfirmation(rawToken))
      .when()
      .post(PASSWORD_RESET_CONFIRM_ENDPOINT)
      .then()
      .extract()
      .response();
  }

  private MailMessage captureMailMessage() {
    var mailCaptor = ArgumentCaptor.forClass(MailMessage.class);
    verify(mailSender).send(mailCaptor.capture());
    return mailCaptor.getValue();
  }

  private String extractResetToken(MailMessage message) {
    var resetLink = message.text()
      .lines()
      .filter(line -> line.startsWith("http"))
      .findFirst()
      .orElseThrow();
    return UriComponentsBuilder
      .fromUriString(resetLink)
      .build()
      .getQueryParams()
      .getFirst("token");
  }

  private static String uniqueEmail() {
    return "password-reset-" + UUID.randomUUID() + "@example.com";
  }

  private static Response awaitResponse(Future<Response> future) {
    try {
      return future.get(10, TimeUnit.SECONDS);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new AssertionError("Interrupted while awaiting password reset response", exception);
    } catch (ExecutionException | TimeoutException exception) {
      throw new AssertionError("Could not obtain password reset response", exception);
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
