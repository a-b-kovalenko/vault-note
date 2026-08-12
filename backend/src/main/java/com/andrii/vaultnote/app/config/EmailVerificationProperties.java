package com.andrii.vaultnote.app.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.email-verification")
public record EmailVerificationProperties(
  String baseUrl,
  Duration tokenTtl) {
}
