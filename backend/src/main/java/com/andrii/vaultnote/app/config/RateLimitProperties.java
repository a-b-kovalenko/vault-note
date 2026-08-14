package com.andrii.vaultnote.app.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.security.rate-limit")
public record RateLimitProperties(
  boolean enabled,
  @Valid @NotNull LoginProperties login,
  @Valid @NotNull RegistrationProperties registration,
  @Valid @NotNull PasswordResetProperties passwordReset,
  @Min(2) int maxEntries) {

  public interface Limits {

    int ipLimit();

    int emailLimit();

    Duration window();
  }

  public record LoginProperties(
    @Min(1) int ipLimit,
    @Min(1) int emailLimit,
    @NotNull Duration window) implements Limits {
  }

  public record RegistrationProperties(
    @Min(1) int ipLimit,
    @Min(1) int emailLimit,
    @NotNull Duration window) implements Limits {
  }

  public record PasswordResetProperties(
    @Min(1) int ipLimit,
    @Min(1) int emailLimit,
    @NotNull Duration window) implements Limits {
  }
}
