package com.andrii.vaultnote.app.security.oauth2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.andrii.vaultnote.app.api.auth.RefreshTokenCookieFactory;
import com.andrii.vaultnote.app.config.OAuth2Properties;
import com.andrii.vaultnote.app.exception.OAuthLoginException;
import com.andrii.vaultnote.app.service.OAuthLoginService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseCookie;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

@ExtendWith(MockitoExtension.class)
class OAuth2AuthenticationSuccessHandlerTest {

  private static final String RAW_REFRESH_TOKEN = "raw-refresh-token";

  @Mock
  OAuthLoginService oauthLoginService;
  @Mock
  RefreshTokenCookieFactory refreshTokenCookieFactory;
  @Mock
  OidcUser oidcUser;

  @Test
  void shouldCreateRefreshCookieAndRedirectToFrontendCallback() throws Exception {
    var properties = new OAuth2Properties("http://localhost:4200");
    var handler = new OAuth2AuthenticationSuccessHandler(
      oauthLoginService,
      refreshTokenCookieFactory,
      properties);
    var request = new MockHttpServletRequest();
    var response = new MockHttpServletResponse();
    var refreshCookie = ResponseCookie.from("vaultnote_refresh_token", RAW_REFRESH_TOKEN)
      .httpOnly(true)
      .build();
    var authentication = new OAuth2AuthenticationToken(
      oidcUser,
      List.of(new SimpleGrantedAuthority("ROLE_USER")),
      "google");

    when(oauthLoginService.login(oidcUser)).thenReturn(RAW_REFRESH_TOKEN);
    when(refreshTokenCookieFactory.create(RAW_REFRESH_TOKEN)).thenReturn(refreshCookie);

    handler.onAuthenticationSuccess(request, response, authentication);

    assertThat(response.getRedirectedUrl()).isEqualTo("http://localhost:4200/oauth/callback");
    assertThat(response.getHeader("Set-Cookie"))
      .contains("vaultnote_refresh_token=" + RAW_REFRESH_TOKEN);
    verify(oauthLoginService).login(oidcUser);
    verify(refreshTokenCookieFactory).create(RAW_REFRESH_TOKEN);
  }

  @Test
  void shouldRedirectToFrontendLoginWhenOAuthLoginFails() throws Exception {
    var properties = new OAuth2Properties("http://localhost:4200/");
    var handler = new OAuth2AuthenticationSuccessHandler(
      oauthLoginService,
      refreshTokenCookieFactory,
      properties);
    var request = new MockHttpServletRequest();
    var response = new MockHttpServletResponse();
    var authentication = new OAuth2AuthenticationToken(
      oidcUser,
      List.of(),
      "google");

    when(oauthLoginService.login(oidcUser)).thenThrow(new OAuthLoginException());

    handler.onAuthenticationSuccess(request, response, authentication);

    assertThat(response.getRedirectedUrl())
      .isEqualTo("http://localhost:4200/login?error=oauth");
  }
}
