package com.andrii.vaultnote.app.security.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import com.andrii.vaultnote.app.config.RateLimitProperties;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.junit.jupiter.api.Test;

@FieldDefaults(level = AccessLevel.PRIVATE)
class InMemoryRateLimitStoreTest {

  static final Instant NOW = Instant.parse("2026-08-14T12:00:00Z");
  static final Duration WINDOW = Duration.ofMinutes(1);

  @Test
  void shouldAllowRequestsUntilLimitIsReached() {
    var store = store(10);
    var rule = rule("login:email:user@example.com", 2);

    assertThat(store.consume(List.of(rule), NOW).allowed()).isTrue();
    assertThat(store.consume(List.of(rule), NOW).allowed()).isTrue();

    var decision = store.consume(List.of(rule), NOW);

    assertThat(decision.allowed()).isFalse();
    assertThat(decision.retryAfter()).isEqualTo(WINDOW);
  }

  @Test
  void shouldNotConsumeOtherRulesWhenOneRuleRejects() {
    var store = store(10);
    var ipRule = rule("login:ip:127.0.0.1", 1);
    var emailRule = rule("login:email:user@example.com", 2);

    assertThat(store.consume(List.of(ipRule, emailRule), NOW).allowed()).isTrue();
    assertThat(store.consume(List.of(ipRule, emailRule), NOW).allowed()).isFalse();
    assertThat(store.consume(List.of(emailRule), NOW).allowed()).isTrue();
    assertThat(store.consume(List.of(emailRule), NOW).allowed()).isFalse();
  }

  @Test
  void shouldAllowRequestAfterWindowExpires() {
    var store = store(10);
    var rule = rule("login:email:user@example.com", 1);

    assertThat(store.consume(List.of(rule), NOW).allowed()).isTrue();
    assertThat(store.consume(List.of(rule), NOW.plus(WINDOW).minusNanos(1)).allowed())
      .isFalse();
    assertThat(store.consume(List.of(rule), NOW.plus(WINDOW)).allowed()).isTrue();
  }

  @Test
  void shouldKeepTheNumberOfCountersBounded() {
    var store = store(2);

    store.consume(List.of(rule("login:email:first@example.com", 1)), NOW);
    store.consume(List.of(rule("login:email:second@example.com", 1)), NOW);
    store.consume(List.of(rule("login:email:third@example.com", 1)), NOW);

    assertThat(store.size()).isEqualTo(2);
  }

  private static InMemoryRateLimitStore store(int maxEntries) {
    var loginProperties = new RateLimitProperties.LoginProperties(10, 10, WINDOW);
    var registrationProperties = new RateLimitProperties.RegistrationProperties(10, 10, WINDOW);
    var passwordResetProperties = new RateLimitProperties.PasswordResetProperties(10, 10, WINDOW);
    var properties = new RateLimitProperties(
      true,
      loginProperties,
      registrationProperties,
      passwordResetProperties,
      maxEntries);
    return new InMemoryRateLimitStore(properties);
  }

  private static RateLimitRule rule(String key, int limit) {
    return new RateLimitRule(key, limit, WINDOW);
  }
}
