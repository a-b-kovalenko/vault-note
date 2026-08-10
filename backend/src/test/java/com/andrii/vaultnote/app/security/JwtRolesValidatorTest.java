package com.andrii.vaultnote.app.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class JwtRolesValidatorTest {

  private final JwtRolesValidator validator = new JwtRolesValidator();

  @Test
  void shouldAcceptKnownRoles() {
    var jwt = jwtWithRoles(List.of("USER", "ADMIN"));

    var result = validator.validate(jwt);

    assertThat(result.hasErrors()).isFalse();
  }

  @Test
  void shouldRejectUnknownRole() {
    var jwt = jwtWithRoles(List.of("USER", "SUPERADMIN"));

    var result = validator.validate(jwt);

    assertThat(result.hasErrors()).isTrue();
  }

  @Test
  void shouldRejectMissingRoles() {
    var jwt = Jwt.withTokenValue("token")
        .header("alg", "HS256")
        .subject("1")
        .build();

    var result = validator.validate(jwt);

    assertThat(result.hasErrors()).isTrue();
  }

  private Jwt jwtWithRoles(List<String> roles) {
    return Jwt.withTokenValue("token")
        .header("alg", "HS256")
        .subject("1")
        .claim("roles", roles)
        .build();
  }
}
