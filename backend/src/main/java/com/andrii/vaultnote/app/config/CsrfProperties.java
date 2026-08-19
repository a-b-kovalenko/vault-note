package com.andrii.vaultnote.app.config;

import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.security.csrf")
public record CsrfProperties(
  @NotNull Mode mode,
  @NotNull Duration tokenTtl) {

  public enum Mode {
    COOKIE, STATELESS
  }
}
