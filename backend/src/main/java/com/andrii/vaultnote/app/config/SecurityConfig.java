package com.andrii.vaultnote.app.config;

import com.andrii.vaultnote.app.security.RolesJwtAuthenticationConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

  private static final int ARGON2_SALT_LENGTH = 16;
  private static final int ARGON2_HASH_LENGTH = 32;
  private static final int ARGON2_PARALLELISM = 1;
  private static final int ARGON2_MEMORY_COST_KIB = 19 * 1024;
  private static final int ARGON2_ITERATIONS = 2;

  @Bean
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      RolesJwtAuthenticationConverter jwtAuthenticationConverter) {
    http
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(session -> session
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(GET, "/actuator/health").permitAll()
            .requestMatchers(GET, "/swagger-ui.html").permitAll()
            .requestMatchers(GET, "/swagger-ui/**").permitAll()
            .requestMatchers(GET, "/v3/api-docs/**").permitAll()
            .requestMatchers(POST, "/api/v1/auth/registrations").permitAll()
            .requestMatchers(POST, "/api/v1/auth/email-verification").permitAll()
            .requestMatchers(POST, "/api/v1/auth/login").permitAll()
            .requestMatchers(POST, "/api/v1/auth/refresh").permitAll()
            .requestMatchers(POST, "/api/v1/auth/logout").permitAll()
            .anyRequest().authenticated())
        .oauth2ResourceServer(oauth2 -> oauth2
            .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)));

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
