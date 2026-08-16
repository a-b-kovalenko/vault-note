package com.andrii.vaultnote.app.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.oauth2.authorization-request-cookie")
public record OAuth2AuthorizationRequestCookieProperties(
  @NotBlank @Pattern(regexp = "^[A-Za-z0-9._-]+$") String cookieName,
  Duration maxAge,
  boolean secure) {
}
