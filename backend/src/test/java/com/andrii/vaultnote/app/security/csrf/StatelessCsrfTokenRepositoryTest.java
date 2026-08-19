package com.andrii.vaultnote.app.security.csrf;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import javax.crypto.spec.SecretKeySpec;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class StatelessCsrfTokenRepositoryTest {

  private static final Instant NOW = Instant.parse("2026-08-19T12:00:00Z");
  private static final SecretKeySpec SIGNING_KEY = new SecretKeySpec(
    "csrf-signing-key-that-is-at-least-32-bytes-long".getBytes(StandardCharsets.UTF_8),
    "HmacSHA256");

  private final StatelessCsrfTokenRepository repository = new StatelessCsrfTokenRepository(
    new StatelessCsrfTokenService(
      SIGNING_KEY,
      Duration.ofMinutes(5),
      Clock.fixed(NOW, ZoneOffset.UTC)));

  @Test
  void shouldGenerateTokenWithAngularHeaderNames() {
    var token = repository.generateToken(new MockHttpServletRequest());

    assertThat(token.getHeaderName()).isEqualTo("X-XSRF-TOKEN");
    assertThat(token.getParameterName()).isEqualTo("_csrf");
    assertThat(token.getToken()).isNotBlank();
  }

  @Test
  void shouldLoadValidTokenFromHeader() {
    var generated = repository.generateToken(new MockHttpServletRequest());
    var request = new MockHttpServletRequest();
    request.addHeader(generated.getHeaderName(), generated.getToken());

    var loaded = repository.loadToken(request);

    assertThat(loaded).isNotNull();
    assertThat(loaded.getToken()).isEqualTo(generated.getToken());
  }

  @Test
  void shouldRejectMissingAndInvalidHeaderToken() {
    var missingTokenRequest = new MockHttpServletRequest();
    var invalidTokenRequest = new MockHttpServletRequest();
    invalidTokenRequest.addHeader("X-XSRF-TOKEN", "invalid-token");

    assertThat(repository.loadToken(missingTokenRequest)).isNull();
    assertThat(repository.loadToken(invalidTokenRequest)).isNull();
  }

  @Test
  void shouldNotPersistTokenInCookie() {
    var request = new MockHttpServletRequest();
    var response = new MockHttpServletResponse();
    var token = repository.generateToken(request);

    repository.saveToken(token, request, response);
    repository.saveToken(null, request, response);

    assertThat(response.getCookies()).isEmpty();
    assertThat(response.getHeader("Set-Cookie")).isNull();
  }

  @Test
  void shouldIgnoreCookiesWhenLoadingToken() {
    var request = new MockHttpServletRequest();
    request.setCookies(new Cookie("XSRF-TOKEN", "cookie-token"));

    assertThat(repository.loadToken(request)).isNull();
  }
}
