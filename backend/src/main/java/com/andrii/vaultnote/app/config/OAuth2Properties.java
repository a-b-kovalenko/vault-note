package com.andrii.vaultnote.app.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.oauth2")
public record OAuth2Properties(
  @NotBlank @Pattern(regexp = "^https?://[^?#]+$") String frontendBaseUrl) {

  private static final String CALLBACK_PATH = "/oauth/callback";
  private static final String LOGIN_ERROR_PATH = "/login?error=oauth";

  public String callbackUrl() {
    return frontendUrl(CALLBACK_PATH);
  }

  public String loginErrorUrl() {
    return frontendUrl(LOGIN_ERROR_PATH);
  }

  private String frontendUrl(String path) {
    var baseUrl = frontendBaseUrl.endsWith("/")
      ? frontendBaseUrl.substring(0, frontendBaseUrl.length() - 1)
      : frontendBaseUrl;
    return baseUrl + path;
  }
}
