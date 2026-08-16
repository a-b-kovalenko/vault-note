package com.andrii.vaultnote.app.config;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.oauth2.google-avatar")
public record GoogleAvatarProperties(
  @NotNull Duration connectTimeout,
  @NotNull Duration requestTimeout,
  @NotNull DataSize maxDownloadSize,
  @NotEmpty Set<String> allowedHosts) {
}
