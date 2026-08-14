package com.andrii.vaultnote.app.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.refresh-token")
public record RefreshTokenProperties(
  Duration ttl,
  @NotBlank @Pattern(regexp = "^[!#$%&'*+\\-.^_`|~0-9A-Za-z]+$") String cookieName,
  String cookiePath,
  boolean secure,
  String sameSite) {
}
