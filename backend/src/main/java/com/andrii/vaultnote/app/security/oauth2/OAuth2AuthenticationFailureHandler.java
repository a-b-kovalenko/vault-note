package com.andrii.vaultnote.app.security.oauth2;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.util.StringUtils;

@Slf4j
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

  private static final String UNKNOWN_ERROR_CODE = "unknown";

  public OAuth2AuthenticationFailureHandler(String defaultFailureUrl) {
    super(defaultFailureUrl);
    setAllowSessionCreation(false);
  }

  @Override
  public void onAuthenticationFailure(
    HttpServletRequest request,
    HttpServletResponse response,
    AuthenticationException exception) throws IOException, ServletException {
    log.warn(
      "OAuth2 authentication failed: exceptionType={}, errorCode={}",
      exception.getClass().getSimpleName(),
      errorCode(exception));
    super.onAuthenticationFailure(request, response, exception);
  }

  private String errorCode(AuthenticationException exception) {
    if (!(exception instanceof OAuth2AuthenticationException oauth2Exception)) {
      return UNKNOWN_ERROR_CODE;
    }

    var errorCode = oauth2Exception.getError().getErrorCode();
    return StringUtils.hasText(errorCode) ? errorCode : UNKNOWN_ERROR_CODE;
  }
}
