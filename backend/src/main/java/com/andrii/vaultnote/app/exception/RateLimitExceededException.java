package com.andrii.vaultnote.app.exception;

import java.time.Duration;
import java.util.Objects;

public class RateLimitExceededException extends RuntimeException {

  public static final String CODE = "RATE_LIMIT_EXCEEDED";

  private final Duration retryAfter;

  public RateLimitExceededException(Duration retryAfter) {
    super("Too many requests. Please try again later.");
    this.retryAfter = Objects.requireNonNull(retryAfter, "Retry-after duration must not be null.");
  }

  public Duration retryAfter() {
    return retryAfter;
  }
}
