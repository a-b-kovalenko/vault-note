package com.andrii.vaultnote.app.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.andrii.vaultnote.app.config.RateLimitProperties;
import com.andrii.vaultnote.app.exception.RateLimitExceededException;
import com.andrii.vaultnote.app.security.ratelimit.RateLimitDecision;
import com.andrii.vaultnote.app.security.ratelimit.RateLimitRule;
import com.andrii.vaultnote.app.security.ratelimit.RateLimitScope;
import com.andrii.vaultnote.app.security.ratelimit.RateLimitStore;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@FieldDefaults(level = AccessLevel.PRIVATE)
class RateLimitServiceImplTest {

  static final Instant NOW = Instant.parse("2026-08-14T12:00:00Z");
  static final Duration WINDOW = Duration.ofMinutes(15);

  @Mock
  RateLimitStore store;
  @Mock
  Clock clock;
  @Captor
  ArgumentCaptor<List<RateLimitRule>> rulesCaptor;

  RateLimitServiceImpl rateLimitService;

  @BeforeEach
  void setUp() {
    var loginProperties = new RateLimitProperties.LoginProperties(5, 7, WINDOW);
    var registrationProperties = new RateLimitProperties.RegistrationProperties(11, 13, WINDOW);
    var passwordResetProperties = new RateLimitProperties.PasswordResetProperties(17, 19, WINDOW);
    var properties = new RateLimitProperties(
      true,
      loginProperties,
      registrationProperties,
      passwordResetProperties,
      100);
    rateLimitService = new RateLimitServiceImpl(properties, store, clock);
  }

  @Test
  void shouldUseRegistrationScopeAndLimits() {
    when(clock.instant()).thenReturn(NOW);
    when(store.consume(anyList(), eq(NOW))).thenReturn(RateLimitDecision.allow());

    rateLimitService.check(
      RateLimitScope.REGISTRATION,
      " 192.168.0.10 ",
      " User@Example.COM ");

    verify(store).consume(rulesCaptor.capture(), eq(NOW));

    assertThat(rulesCaptor.getValue())
      .extracting(RateLimitRule::key)
      .containsExactly(
        "registration:ip:192.168.0.10",
        "registration:email:user@example.com");
    assertThat(rulesCaptor.getValue())
      .extracting(RateLimitRule::limit)
      .containsExactly(11, 13);
  }

  @Test
  void shouldNormalizeEmailAndClientIpBeforeCheckingLimits() {
    when(clock.instant()).thenReturn(NOW);
    when(store.consume(anyList(), eq(NOW))).thenReturn(RateLimitDecision.allow());

    rateLimitService.check(RateLimitScope.LOGIN, " 127.0.0.1 ", " User@Example.COM ");

    verify(store).consume(rulesCaptor.capture(), eq(NOW));

    assertThat(rulesCaptor.getValue())
      .extracting(RateLimitRule::key)
      .containsExactly(
        "login:ip:127.0.0.1",
        "login:email:user@example.com");
    assertThat(rulesCaptor.getValue())
      .extracting(RateLimitRule::limit)
      .containsExactly(5, 7);
  }

  @Test
  void shouldUsePasswordResetScopeAndLimits() {
    when(clock.instant()).thenReturn(NOW);
    when(store.consume(anyList(), eq(NOW))).thenReturn(RateLimitDecision.allow());

    rateLimitService.check(
      RateLimitScope.PASSWORD_RESET,
      " 127.0.0.1 ",
      " User@Example.COM ");

    verify(store).consume(rulesCaptor.capture(), eq(NOW));

    assertThat(rulesCaptor.getValue())
      .extracting(RateLimitRule::key)
      .containsExactly(
        "password-reset:ip:127.0.0.1",
        "password-reset:email:user@example.com");
    assertThat(rulesCaptor.getValue())
      .extracting(RateLimitRule::limit)
      .containsExactly(17, 19);
  }

  @Test
  void shouldThrowRateLimitExceptionWhenStoreRejects() {
    var retryAfter = Duration.ofSeconds(42);
    when(clock.instant()).thenReturn(NOW);
    when(store.consume(anyList(), eq(NOW))).thenReturn(RateLimitDecision.rejected(retryAfter));

    assertThatExceptionOfType(RateLimitExceededException.class)
      .isThrownBy(() -> rateLimitService.check(
        RateLimitScope.LOGIN,
        "127.0.0.1",
        "user@example.com"))
      .satisfies(exception -> assertThat(exception.retryAfter()).isEqualTo(retryAfter));
  }

  @Test
  void shouldSkipStoreWhenRateLimitingIsDisabled() {
    var loginProperties = new RateLimitProperties.LoginProperties(5, 7, WINDOW);
    var registrationProperties = new RateLimitProperties.RegistrationProperties(11, 13, WINDOW);
    var passwordResetProperties = new RateLimitProperties.PasswordResetProperties(17, 19, WINDOW);
    var disabledProperties = new RateLimitProperties(
      false,
      loginProperties,
      registrationProperties,
      passwordResetProperties,
      100);
    var disabledService = new RateLimitServiceImpl(disabledProperties, store, clock);

    disabledService.check(RateLimitScope.LOGIN, "127.0.0.1", "user@example.com");

    verifyNoInteractions(store, clock);
  }
}
