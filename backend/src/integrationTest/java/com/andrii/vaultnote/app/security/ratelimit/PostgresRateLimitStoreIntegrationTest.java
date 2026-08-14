package com.andrii.vaultnote.app.security.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.andrii.vaultnote.app.exception.RateLimitExceededException;
import com.andrii.vaultnote.app.service.RateLimitService;
import com.andrii.vaultnote.support.AbstractBaseIntegrationTest;
import com.github.database.rider.core.api.dataset.DataSet;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

@DataSet(value = "auth-baseline.yml", skipCleaningFor = {"databasechangelog", "databasechangeloglock"})
@TestPropertySource(
  properties = {
    "app.security.rate-limit.registration.ip-limit=100",
    "app.security.rate-limit.registration.email-limit=2",
    "app.security.rate-limit.registration.window=PT1M"
  })
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
class PostgresRateLimitStoreIntegrationTest extends AbstractBaseIntegrationTest {

  private static final String EMAIL = "postgres-rate-limit@example.com";

  RateLimitService rateLimitService;
  JdbcTemplate jdbcTemplate;

  @Autowired
  PostgresRateLimitStoreIntegrationTest(
    RateLimitService rateLimitService,
    JdbcTemplate jdbcTemplate) {
    this.rateLimitService = rateLimitService;
    this.jdbcTemplate = jdbcTemplate;
  }

  @Test
  void shouldShareAtomicCountersThroughPostgres() {
    rateLimitService.checkRegistration("127.0.0.1", EMAIL);
    rateLimitService.checkRegistration("127.0.0.1", " " + EMAIL.toUpperCase() + " ");

    assertThatThrownBy(() -> rateLimitService.checkRegistration("127.0.0.1", EMAIL))
      .isInstanceOf(RateLimitExceededException.class)
      .satisfies(exception -> assertThat(((RateLimitExceededException) exception).retryAfter())
        .isBetween(java.time.Duration.ofSeconds(1), java.time.Duration.ofMinutes(1)));

    assertThat(jdbcTemplate.queryForObject(
      "SELECT COUNT(*) FROM vaultnote.rate_limit_counters",
      Integer.class)).isEqualTo(2);
  }
}
