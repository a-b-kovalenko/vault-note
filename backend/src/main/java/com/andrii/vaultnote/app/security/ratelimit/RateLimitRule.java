package com.andrii.vaultnote.app.security.ratelimit;

import java.time.Duration;
import java.util.Objects;

public record RateLimitRule(String key, int limit, Duration window) {

  public RateLimitRule {
    if (key == null || key.isBlank()) {
      throw new IllegalArgumentException("Rate-limit key must not be blank.");
    }
    if (limit < 1) {
      throw new IllegalArgumentException("Rate-limit limit must be positive.");
    }
    Objects.requireNonNull(window, "Rate-limit window must not be null.");
    if (window.isZero() || window.isNegative()) {
      throw new IllegalArgumentException("Rate-limit window must be positive.");
    }
  }
}
