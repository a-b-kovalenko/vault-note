package com.andrii.vaultnote.app.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.refresh-token")
public record RefreshTokenProperties(
  Duration ttl,
  String cookieName,
  String cookiePath,
  boolean secure,
  String sameSite) {
}
