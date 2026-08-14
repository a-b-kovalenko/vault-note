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
  @Valid @NotNull RegistrationProperties registration) {

  public record LoginProperties(
    @Min(1) int ipLimit,
    @Min(1) int emailLimit,
    @NotNull Duration window) {
  }

  public record RegistrationProperties(
    @Min(1) int ipLimit,
    @Min(1) int emailLimit,
    @NotNull Duration window) {
  }
}
