package com.andrii.vaultnote.app.config;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class JwtConfigurationTest {

  private static final String KNOWN_DEVELOPMENT_SECRET = "local-development-only-secret-change-me-please";

  private final JwtConfiguration configuration = new JwtConfiguration();

  @Test
  void shouldRejectMissingSecret() {
    assertThatThrownBy(() -> configuration.jwtSecretKey(properties(null)))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("JWT secret must be configured");
  }

  @Test
  void shouldRejectBlankSecret() {
    assertThatThrownBy(() -> configuration.jwtSecretKey(properties(" ")))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("JWT secret must be configured");
  }

  @Test
  void shouldRejectKnownDevelopmentSecret() {
    assertThatThrownBy(() -> configuration.jwtSecretKey(properties(KNOWN_DEVELOPMENT_SECRET)))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("JWT secret must not use the known development placeholder");
  }

  @Test
  void shouldRejectShortSecret() {
    assertThatThrownBy(() -> configuration.jwtSecretKey(properties("too-short")))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("JWT secret must contain at least 32 characters");
  }

  private JwtProperties properties(String secret) {
    return new JwtProperties("vaultnote", secret, Duration.ofMinutes(15));
  }
}
