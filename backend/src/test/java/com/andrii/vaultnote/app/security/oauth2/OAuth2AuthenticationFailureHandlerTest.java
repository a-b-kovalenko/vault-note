package com.andrii.vaultnote.app.security.oauth2;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

@ExtendWith(OutputCaptureExtension.class)
class OAuth2AuthenticationFailureHandlerTest {

  private static final String LOGIN_ERROR_URL = "http://localhost:4200/login?error=oauth";

  @Test
  void shouldLogSafeFailureDetailsAndRedirectToFrontendLogin(CapturedOutput output) throws Exception {
    var handler = new OAuth2AuthenticationFailureHandler(LOGIN_ERROR_URL);
    var request = new MockHttpServletRequest();
    var response = new MockHttpServletResponse();
    var exception = new OAuth2AuthenticationException(
      new OAuth2Error("invalid_grant"),
      "authorization code=secret-code");

    handler.onAuthenticationFailure(request, response, exception);

    assertThat(response.getRedirectedUrl()).isEqualTo(LOGIN_ERROR_URL);
    assertThat(output)
      .contains("OAuth2 authentication failed")
      .contains("errorCode=invalid_grant")
      .doesNotContain("secret-code");
  }
}
