package com.andrii.vaultnote.app.security.oauth2;

import com.andrii.vaultnote.app.api.auth.RefreshTokenCookieFactory;
import com.andrii.vaultnote.app.config.OAuth2Properties;
import com.andrii.vaultnote.app.exception.OAuthLoginException;
import com.andrii.vaultnote.app.service.OAuthLoginService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

  private static final String GOOGLE_REGISTRATION_ID = "google";

  OAuthLoginService oauthLoginService;
  RefreshTokenCookieFactory refreshTokenCookieFactory;
  OAuth2Properties oauth2Properties;
  RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

  @Override
  public void onAuthenticationSuccess(
    HttpServletRequest request,
    HttpServletResponse response,
    Authentication authentication) throws IOException, ServletException {
    if (!(authentication instanceof OAuth2AuthenticationToken oauth2Authentication)
      || !GOOGLE_REGISTRATION_ID.equals(oauth2Authentication.getAuthorizedClientRegistrationId())
      || !(oauth2Authentication.getPrincipal() instanceof OidcUser oidcUser)) {
      redirectToLoginError(request, response);
      return;
    }

    try {
      var rawRefreshToken = oauthLoginService.login(oidcUser);
      response.addHeader(
        HttpHeaders.SET_COOKIE,
        refreshTokenCookieFactory.create(rawRefreshToken).toString());
      redirectStrategy.sendRedirect(request, response, oauth2Properties.callbackUrl());
    } catch (OAuthLoginException exception) {
      log.warn("OAuth sign-in could not be completed");
      redirectToLoginError(request, response);
    }
  }

  private void redirectToLoginError(
    HttpServletRequest request,
    HttpServletResponse response) throws IOException {
    redirectStrategy.sendRedirect(request, response, oauth2Properties.loginErrorUrl());
  }
}
