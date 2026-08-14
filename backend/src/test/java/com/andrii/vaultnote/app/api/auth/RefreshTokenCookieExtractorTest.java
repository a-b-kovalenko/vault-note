package com.andrii.vaultnote.app.api.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.andrii.vaultnote.app.config.RefreshTokenProperties;
import jakarta.servlet.http.Cookie;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class RefreshTokenCookieExtractorTest {

  private static final String COOKIE_NAME = "custom_refresh_token";
  private static final String RAW_REFRESH_TOKEN = "raw-refresh-token";

  private final RefreshTokenCookieExtractor extractor = new RefreshTokenCookieExtractor(
    new RefreshTokenProperties(
      Duration.ofDays(7),
      COOKIE_NAME,
      "/api/v1/auth",
      false,
      "Lax"));

  @Test
  void shouldExtractCookieWithConfiguredName() {
    var request = new MockHttpServletRequest();
    request.setCookies(
      new Cookie("other_cookie", "ignored"),
      new Cookie(COOKIE_NAME, RAW_REFRESH_TOKEN));

    assertThat(extractor.extract(request)).contains(RAW_REFRESH_TOKEN);
  }

  @Test
  void shouldIgnoreCookieWithDifferentName() {
    var request = new MockHttpServletRequest();
    request.setCookies(new Cookie("vaultnote_refresh_token", RAW_REFRESH_TOKEN));

    assertThat(extractor.extract(request)).isEmpty();
  }

  @Test
  void shouldReturnEmptyWhenRequestHasNoCookies() {
    var request = new MockHttpServletRequest();

    assertThat(extractor.extract(request)).isEmpty();
  }

}
