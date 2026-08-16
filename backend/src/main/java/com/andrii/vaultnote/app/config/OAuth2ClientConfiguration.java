package com.andrii.vaultnote.app.config;

import com.andrii.vaultnote.app.api.auth.RefreshTokenCookieFactory;
import com.andrii.vaultnote.app.security.oauth2.CookieOAuth2AuthorizationRequestRepository;
import com.andrii.vaultnote.app.security.oauth2.OAuth2AuthenticationSuccessHandler;
import com.andrii.vaultnote.app.service.OAuthLoginService;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.crypto.SecretKey;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

@Configuration
@EnableConfigurationProperties({
  OAuth2AuthorizationRequestCookieProperties.class,
  OAuth2Properties.class
})
public class OAuth2ClientConfiguration {

  @Bean
  public AuthorizationRequestRepository<OAuth2AuthorizationRequest> authorizationRequestRepository(
    SecretKey jwtSecretKey,
    OAuth2AuthorizationRequestCookieProperties properties) {
    return new CookieOAuth2AuthorizationRequestRepository(
      new ObjectMapper(),
      jwtSecretKey,
      properties);
  }

  @Bean
  public AuthenticationSuccessHandler oauth2AuthenticationSuccessHandler(
    OAuthLoginService oauthLoginService,
    RefreshTokenCookieFactory refreshTokenCookieFactory,
    OAuth2Properties oauth2Properties) {
    return new OAuth2AuthenticationSuccessHandler(
      oauthLoginService,
      refreshTokenCookieFactory,
      oauth2Properties);
  }

  @Bean
  public AuthenticationFailureHandler oauth2AuthenticationFailureHandler(
    OAuth2Properties oauth2Properties) {
    var handler = new SimpleUrlAuthenticationFailureHandler(oauth2Properties.loginErrorUrl());
    handler.setAllowSessionCreation(false);
    return handler;
  }
}
