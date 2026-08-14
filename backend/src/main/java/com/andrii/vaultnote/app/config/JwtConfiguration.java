package com.andrii.vaultnote.app.config;

import static java.util.Objects.isNull;

import com.andrii.vaultnote.app.security.JwtRolesValidator;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@Configuration
class JwtConfiguration {

  private static final int MIN_SECRET_LENGTH = 32;
  private static final String KNOWN_DEVELOPMENT_SECRET = "local-development-only-secret-change-me-please";

  @Bean
  SecretKey jwtSecretKey(JwtProperties properties) {
    var secret = properties.secret();

    if (isNull(secret) || secret.isBlank()) {
      throw new IllegalStateException("JWT secret must be configured");
    }
    if (KNOWN_DEVELOPMENT_SECRET.equals(secret)) {
      throw new IllegalStateException("JWT secret must not use the known development placeholder");
    }
    if (secret.length() < MIN_SECRET_LENGTH) {
      throw new IllegalStateException("JWT secret must contain at least 32 characters");
    }
    return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
  }

  @Bean
  JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
    return NimbusJwtEncoder.withSecretKey(jwtSecretKey)
      .algorithm(MacAlgorithm.HS256)
      .build();
  }

  @Bean
  JwtDecoder jwtDecoder(
    SecretKey jwtSecretKey,
    JwtProperties properties,
    JwtRolesValidator jwtRolesValidator) {
    var decoder = NimbusJwtDecoder.withSecretKey(jwtSecretKey)
      .macAlgorithm(MacAlgorithm.HS256)
      .build();
    decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
      JwtValidators.createDefaultWithIssuer(properties.issuer()),
      jwtRolesValidator));
    return decoder;
  }

}
