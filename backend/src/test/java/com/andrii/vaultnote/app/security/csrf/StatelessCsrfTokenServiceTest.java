package com.andrii.vaultnote.app.security.csrf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

class StatelessCsrfTokenServiceTest {

  private static final Instant NOW = Instant.parse("2026-08-19T12:00:00Z");
  private static final Duration TOKEN_TTL = Duration.ofMinutes(5);
  private static final SecretKeySpec SIGNING_KEY = key(
    "csrf-signing-key-that-is-at-least-32-bytes-long");

  private final StatelessCsrfTokenService service = serviceAt(NOW, SIGNING_KEY);

  @Test
  void shouldGenerateAndValidateToken() {
    var token = service.generateToken();

    assertThat(token).matches("v1\\.[0-9]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+");
    assertThat(service.isValid(token)).isTrue();
  }

  @Test
  void shouldGenerateDifferentTokens() {
    var firstToken = service.generateToken();
    var secondToken = service.generateToken();

    assertThat(secondToken).isNotEqualTo(firstToken);
    assertThat(service.isValid(firstToken)).isTrue();
    assertThat(service.isValid(secondToken)).isTrue();
  }

  @Test
  void shouldRejectTokenWithTamperedPayload() {
    var token = service.generateToken();
    var parts = token.split("\\.", -1);
    var tamperedToken = String.join(".", parts[0], parts[1], parts[2] + "A", parts[3]);

    assertThat(service.isValid(tamperedToken)).isFalse();
  }

  @Test
  void shouldRejectTokenWithTamperedSignature() {
    var token = service.generateToken();
    var parts = token.split("\\.", -1);
    var signature = Base64.getUrlDecoder().decode(parts[3]);
    signature[0] ^= 0x01;
    var tamperedSignature = Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
    var tamperedToken = String.join(".", parts[0], parts[1], parts[2], tamperedSignature);

    assertThat(service.isValid(tamperedToken)).isFalse();
  }

  @Test
  void shouldRejectTokenSignedWithAnotherKey() {
    var token = service.generateToken();
    var anotherService = serviceAt(NOW, key("another-csrf-signing-key-that-is-long-enough"));

    assertThat(anotherService.isValid(token)).isFalse();
  }

  @Test
  void shouldRejectExpiredToken() {
    var token = service.generateToken();
    var expiredService = serviceAt(NOW.plus(TOKEN_TTL), SIGNING_KEY);

    assertThat(expiredService.isValid(token)).isFalse();
  }

  @Test
  void shouldRejectMalformedToken() {
    assertThat(service.isValid("not-a-token")).isFalse();
    assertThat(service.isValid("v1.invalid.nonce.signature")).isFalse();
    assertThat(service.isValid(null)).isFalse();
    assertThat(service.isValid(" ")).isFalse();
  }

  @Test
  void shouldRejectWeakSigningKey() {
    assertThatThrownBy(() -> serviceAt(NOW, key("too-short")))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("CSRF signing key must contain at least 32 bytes");
  }

  @Test
  void shouldRejectNonPositiveTtl() {
    assertThatThrownBy(() -> new StatelessCsrfTokenService(
      SIGNING_KEY,
      Duration.ZERO,
      Clock.fixed(NOW, ZoneOffset.UTC)))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("CSRF token TTL must contain at least one second");
  }

  private static StatelessCsrfTokenService serviceAt(Instant instant, SecretKeySpec key) {
    return new StatelessCsrfTokenService(
      key,
      TOKEN_TTL,
      Clock.fixed(instant, ZoneOffset.UTC));
  }

  private static SecretKeySpec key(String value) {
    return new SecretKeySpec(value.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
  }
}
