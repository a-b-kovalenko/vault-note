package com.andrii.vaultnote.app.security.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.andrii.vaultnote.app.security.SecureTokenGenerator;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

@ExtendWith(MockitoExtension.class)
@FieldDefaults(level = AccessLevel.PRIVATE)
class PostgresRateLimitStoreTest {

  static final Instant NOW = Instant.parse("2026-08-14T12:00:00Z");
  static final Duration WINDOW = Duration.ofMinutes(1);

  @Mock
  JdbcTemplate jdbcTemplate;
  @Mock
  PlatformTransactionManager transactionManager;
  @Mock
  TransactionStatus transactionStatus;

  PostgresRateLimitStore store;

  @BeforeEach
  void setUp() {
    lenient().when(transactionManager.getTransaction(any(TransactionDefinition.class)))
      .thenReturn(transactionStatus);
    lenient().when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(1);
    store = new PostgresRateLimitStore(
      jdbcTemplate,
      new SecureTokenGenerator(),
      transactionManager);
  }

  @Test
  void shouldAtomicallyAllowAllRulesWhenNoneIsExceeded() {
    doReturn(new PostgresRateLimitStore.Counter(0, NOW))
      .when(jdbcTemplate)
      .queryForObject(
        eq(PostgresRateLimitStore.SELECT_COUNTER_SQL),
        ArgumentMatchers.<RowMapper<PostgresRateLimitStore.Counter>>any(),
        any(Object[].class));

    var decision = store.consume(
      List.of(rule("registration:email:user@example.com", 2), rule("registration:ip:127.0.0.1", 2)),
      NOW);

    assertThat(decision).isEqualTo(RateLimitDecision.allow());
    verify(transactionStatus, never()).setRollbackOnly();
  }

  @Test
  void shouldRollbackAllRulesWhenOneRuleIsExceeded() {
    when(jdbcTemplate.queryForObject(
      eq(PostgresRateLimitStore.SELECT_COUNTER_SQL),
      ArgumentMatchers.<RowMapper<PostgresRateLimitStore.Counter>>any(),
      any(Object[].class)))
      .thenReturn(
        new PostgresRateLimitStore.Counter(0, NOW),
        new PostgresRateLimitStore.Counter(1, NOW.plus(WINDOW)));

    var decision = store.consume(
      List.of(rule("registration:email:user@example.com", 2), rule("registration:ip:127.0.0.1", 1)),
      NOW);

    assertThat(decision.allowed()).isFalse();
    assertThat(decision.retryAfter()).isEqualTo(WINDOW);
    verify(transactionStatus).setRollbackOnly();
  }

  @Test
  void shouldStartANewWindowWhenExistingCounterExpired() {
    doReturn(new PostgresRateLimitStore.Counter(10, NOW.minusSeconds(1)))
      .when(jdbcTemplate)
      .queryForObject(
        eq(PostgresRateLimitStore.SELECT_COUNTER_SQL),
        ArgumentMatchers.<RowMapper<PostgresRateLimitStore.Counter>>any(),
        any(Object[].class));

    var decision = store.consume(List.of(rule("login:ip:127.0.0.1", 1)), NOW);

    assertThat(decision).isEqualTo(RateLimitDecision.allow());
    verify(transactionStatus, never()).setRollbackOnly();
  }

  @Test
  void shouldAllowEmptyRuleListWithoutOpeningTransaction() {
    var decision = store.consume(List.of(), NOW);

    assertThat(decision).isEqualTo(RateLimitDecision.allow());
    verify(transactionManager, never()).getTransaction(any());
  }

  @Test
  void shouldFailWhenCounterDisappearsDuringTransaction() {
    doReturn(null)
      .when(jdbcTemplate)
      .queryForObject(
        eq(PostgresRateLimitStore.SELECT_COUNTER_SQL),
        ArgumentMatchers.<RowMapper<PostgresRateLimitStore.Counter>>any(),
        any(Object[].class));

    assertThatThrownBy(() -> store.consume(List.of(rule("login:ip:127.0.0.1", 1)), NOW))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Rate-limit counter was not found after insertion.");
  }

  private static RateLimitRule rule(String key, int limit) {
    return new RateLimitRule(key, limit, WINDOW);
  }
}
