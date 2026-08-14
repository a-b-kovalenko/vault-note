package com.andrii.vaultnote.app.security.ratelimit;

import java.time.Duration;
import java.util.Objects;

public record RateLimitDecision(boolean allowed, Duration retryAfter) {

  public static RateLimitDecision allow() {
    return new RateLimitDecision(true, Duration.ZERO);
  }

  public static RateLimitDecision rejected(Duration retryAfter) {
    var nonNullRetryAfter = Objects.requireNonNull(retryAfter, "Retry-after duration must not be null.");
    if (nonNullRetryAfter.isZero() || nonNullRetryAfter.isNegative()) {
      throw new IllegalArgumentException("Retry-after duration must be positive.");
    }
    return new RateLimitDecision(false, nonNullRetryAfter);
  }
}
