package com.andrii.vaultnote.app.config;

import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@Configuration
class JwtConfiguration {

  private static final int MIN_SECRET_LENGTH = 32;

  @Bean
  SecretKey jwtSecretKey(JwtProperties properties) {
    var secret = properties.secret();
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
}
