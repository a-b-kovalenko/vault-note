package com.andrii.vaultnote.app.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.password-reset")
public record PasswordResetProperties(
  String baseUrl,
  Duration tokenTtl) {
}
