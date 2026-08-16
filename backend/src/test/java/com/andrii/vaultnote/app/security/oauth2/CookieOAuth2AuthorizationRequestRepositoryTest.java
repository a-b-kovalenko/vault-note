package com.andrii.vaultnote.app.security.oauth2;

import static org.assertj.core.api.Assertions.assertThat;

import com.andrii.vaultnote.app.config.OAuth2AuthorizationRequestCookieProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Set;
import javax.crypto.spec.SecretKeySpec;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.core.endpoint.PkceParameterNames;

class CookieOAuth2AuthorizationRequestRepositoryTest {

  private static final String COOKIE_NAME = "vaultnote_oauth2_authorization_request";
  private static final String CALLBACK_URI = "http://localhost:8080/login/oauth2/code/google";

  private final CookieOAuth2AuthorizationRequestRepository repository = new CookieOAuth2AuthorizationRequestRepository(
    new ObjectMapper(),
    new SecretKeySpec(
      "integration-test-jwt-secret-that-is-long-enough".getBytes(StandardCharsets.UTF_8),
      "HmacSHA256"),
    new OAuth2AuthorizationRequestCookieProperties(
      COOKIE_NAME,
      Duration.ofMinutes(5),
      false));

  @Test
  void shouldRoundTripAuthorizationRequestWithStateAndPkce() {
    var request = authorizationRequest();
    var saveResponse = new MockHttpServletResponse();

    repository.saveAuthorizationRequest(request, new MockHttpServletRequest(), saveResponse);

    var cookieValue = cookieValue(saveResponse);
    assertThat(cookieValue)
      .doesNotContain("state-value")
      .doesNotContain("verifier-value");
    var loadRequest = new MockHttpServletRequest();
    loadRequest.setCookies(new Cookie(COOKIE_NAME, cookieValue));

    var restored = repository.loadAuthorizationRequest(loadRequest);

    assertThat(restored).isNotNull();
    assertThat(restored.getState()).isEqualTo("state-value");
    assertThat(restored.getScopes()).containsExactlyInAnyOrder("openid", "profile", "email");
    assertThat(restored.getAdditionalParameters())
      .containsEntry(PkceParameterNames.CODE_CHALLENGE, "challenge-value")
      .containsEntry(PkceParameterNames.CODE_CHALLENGE_METHOD, "S256");
    assertThat((String) restored.getAttribute(PkceParameterNames.CODE_VERIFIER))
      .isEqualTo("verifier-value");
  }

  @Test
  void shouldRejectTamperedAuthorizationRequestCookie() {
    var saveResponse = new MockHttpServletResponse();
    repository.saveAuthorizationRequest(
      authorizationRequest(),
      new MockHttpServletRequest(),
      saveResponse);

    var cookieValue = cookieValue(saveResponse);
    var tamperedValue = cookieValue.substring(0, cookieValue.length() - 1)
      + (cookieValue.endsWith("A") ? "B" : "A");
    var request = new MockHttpServletRequest();
    request.setCookies(new Cookie(COOKIE_NAME, tamperedValue));

    assertThat(repository.loadAuthorizationRequest(request)).isNull();
  }

  @Test
  void shouldRemoveAuthorizationRequestAndExpireCookie() {
    var saveResponse = new MockHttpServletResponse();
    repository.saveAuthorizationRequest(
      authorizationRequest(),
      new MockHttpServletRequest(),
      saveResponse);
    var request = new MockHttpServletRequest();
    request.setCookies(new Cookie(COOKIE_NAME, cookieValue(saveResponse)));
    var removeResponse = new MockHttpServletResponse();

    var removed = repository.removeAuthorizationRequest(request, removeResponse);

    assertThat(removed).isNotNull();
    assertThat(removeResponse.getHeader("Set-Cookie"))
      .contains(COOKIE_NAME + "=")
      .contains("Max-Age=0")
      .contains("Path=/login/oauth2/code")
      .contains("HttpOnly")
      .contains("SameSite=Lax");
  }

  private OAuth2AuthorizationRequest authorizationRequest() {
    return OAuth2AuthorizationRequest.authorizationCode()
      .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
      .clientId("client-id")
      .redirectUri(CALLBACK_URI)
      .scopes(Set.of("openid", "profile", "email"))
      .state("state-value")
      .additionalParameters(parameters -> {
        parameters.put(PkceParameterNames.CODE_CHALLENGE, "challenge-value");
        parameters.put(PkceParameterNames.CODE_CHALLENGE_METHOD, "S256");
      })
      .attributes(attributes -> {
        attributes.put(OAuth2ParameterNames.REGISTRATION_ID, "google");
        attributes.put(PkceParameterNames.CODE_VERIFIER, "verifier-value");
      })
      .build();
  }

  private String cookieValue(MockHttpServletResponse response) {
    var header = response.getHeader("Set-Cookie");
    var prefix = COOKIE_NAME + "=";
    var start = header.indexOf(prefix) + prefix.length();
    var end = header.indexOf(';', start);
    return header.substring(start, end);
  }
}
