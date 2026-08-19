package com.andrii.vaultnote.app.config;

import com.andrii.vaultnote.app.security.csrf.StatelessCsrfTokenRepository;
import com.andrii.vaultnote.app.security.csrf.StatelessCsrfTokenService;
import java.time.Clock;
import javax.crypto.SecretKey;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.CsrfTokenRepository;

@Configuration
@EnableConfigurationProperties(CsrfProperties.class)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CsrfConfiguration {

  @Bean
  public CsrfTokenRepository csrfTokenRepository(
    CsrfProperties properties,
    SecretKey jwtSecretKey,
    Clock clock) {
    if (properties.mode() == CsrfProperties.Mode.STATELESS) {
      var tokenService = new StatelessCsrfTokenService(
        jwtSecretKey,
        properties.tokenTtl(),
        clock);
      return new StatelessCsrfTokenRepository(tokenService);
    }

    var repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
    repository.setCookiePath("/");
    repository.setCookieCustomizer(builder -> builder.sameSite("Lax"));
    return repository;
  }

  @Bean
  public CsrfTokenRequestHandler csrfTokenRequestHandler() {
    var handler = new CsrfTokenRequestAttributeHandler();
    handler.setCsrfRequestAttributeName(null);
    return handler;
  }

}
