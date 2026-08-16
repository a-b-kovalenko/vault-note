package com.andrii.vaultnote.app.config;

import com.andrii.vaultnote.app.security.oauth2.CookieOAuth2AuthorizationRequestRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.crypto.SecretKey;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

@Configuration
@EnableConfigurationProperties(OAuth2AuthorizationRequestCookieProperties.class)
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
}
