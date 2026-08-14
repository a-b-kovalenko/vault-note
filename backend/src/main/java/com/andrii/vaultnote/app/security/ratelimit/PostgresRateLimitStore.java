package com.andrii.vaultnote.app.security.ratelimit;

import com.andrii.vaultnote.app.security.SecureTokenGenerator;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

@Component
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PostgresRateLimitStore implements RateLimitStore {

  static final String INSERT_COUNTER_SQL = """
    INSERT INTO vaultnote.rate_limit_counters (rate_limit_key_hash, request_count, expires_at)
    VALUES (?, 0, ?)
    ON CONFLICT (rate_limit_key_hash) DO NOTHING
    """;
  static final String SELECT_COUNTER_SQL = """
    SELECT request_count, expires_at
    FROM vaultnote.rate_limit_counters
    WHERE rate_limit_key_hash = ?
    FOR UPDATE
    """;
  static final String UPDATE_COUNTER_SQL = """
    UPDATE vaultnote.rate_limit_counters
    SET request_count = ?, expires_at = ?
    WHERE rate_limit_key_hash = ?
    """;
  static final RowMapper<Counter> COUNTER_ROW_MAPPER = (resultSet, rowNumber) -> new Counter(
    resultSet.getInt("request_count"),
    resultSet.getTimestamp("expires_at").toInstant());

  JdbcTemplate jdbcTemplate;
  SecureTokenGenerator secureTokenGenerator;
  TransactionTemplate transactionTemplate;

  public PostgresRateLimitStore(
    JdbcTemplate jdbcTemplate,
    SecureTokenGenerator secureTokenGenerator,
    PlatformTransactionManager transactionManager) {
    this.jdbcTemplate = jdbcTemplate;
    this.secureTokenGenerator = secureTokenGenerator;
    this.transactionTemplate = new TransactionTemplate(transactionManager);
    this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
  }

  @Override
  public RateLimitDecision consume(List<RateLimitRule> rules, Instant now) {
    Objects.requireNonNull(rules, "Rate-limit rules must not be null.");
    Objects.requireNonNull(now, "Current time must not be null.");
    if (rules.isEmpty()) {
      return RateLimitDecision.allow();
    }

    return Objects.requireNonNull(
      transactionTemplate.execute(status -> consumeInTransaction(status, rules, now)));
  }

  private RateLimitDecision consumeInTransaction(
    TransactionStatus status,
    List<RateLimitRule> rules,
    Instant now) {
    var states = new ArrayList<CounterState>();
    var longestRetryAfter = Duration.ZERO;

    for (var rule : rules.stream().sorted(Comparator.comparing(RateLimitRule::key)).toList()) {
      var keyHash = secureTokenGenerator.hash(rule.key());
      jdbcTemplate.update(INSERT_COUNTER_SQL, keyHash, timestamp(now));
      var counter = jdbcTemplate.queryForObject(SELECT_COUNTER_SQL, COUNTER_ROW_MAPPER, keyHash);
      if (counter == null) {
        throw new IllegalStateException("Rate-limit counter was not found after insertion.");
      }

      var requestCount = counter.requestCount();
      var expiresAt = counter.expiresAt();
      if (!expiresAt.isAfter(now)) {
        requestCount = 0;
        expiresAt = now.plus(rule.window());
      }

      if (requestCount >= rule.limit()) {
        var retryAfter = retryAfter(now, expiresAt);
        if (retryAfter.compareTo(longestRetryAfter) > 0) {
          longestRetryAfter = retryAfter;
        }
      }

      states.add(new CounterState(keyHash, requestCount, expiresAt));
    }

    if (!longestRetryAfter.isZero()) {
      status.setRollbackOnly();
      return RateLimitDecision.rejected(longestRetryAfter);
    }

    for (var state : states) {
      jdbcTemplate.update(
        UPDATE_COUNTER_SQL,
        state.requestCount() + 1,
        timestamp(state.expiresAt()),
        state.keyHash());
    }

    return RateLimitDecision.allow();
  }

  private static Timestamp timestamp(Instant instant) {
    return Timestamp.from(instant);
  }

  private static Duration retryAfter(Instant now, Instant expiresAt) {
    var retryAfter = Duration.between(now, expiresAt);
    return retryAfter.compareTo(Duration.ZERO) > 0 ? retryAfter : Duration.ofSeconds(1);
  }

  record Counter(int requestCount, Instant expiresAt) {
  }

  private record CounterState(String keyHash, int requestCount, Instant expiresAt) {
  }
}
