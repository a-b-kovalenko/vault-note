package com.andrii.vaultnote.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

  private static final int ARGON2_SALT_LENGTH = 16;
  private static final int ARGON2_HASH_LENGTH = 32;
  private static final int ARGON2_PARALLELISM = 1;
  private static final int ARGON2_MEMORY_COST_KIB = 19 * 1024;
  private static final int ARGON2_ITERATIONS = 2;

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) {
    http
        .csrf(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
            .requestMatchers("/api/v1/auth/registrations").permitAll()
            .anyRequest().authenticated());

    return http.build();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new Argon2PasswordEncoder(
        ARGON2_SALT_LENGTH,
        ARGON2_HASH_LENGTH,
        ARGON2_PARALLELISM,
        ARGON2_MEMORY_COST_KIB,
        ARGON2_ITERATIONS);
  }
}
