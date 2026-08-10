package com.andrii.vaultnote.app.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

class RolesJwtAuthenticationConverterTest {

  private final RolesJwtAuthenticationConverter converter = new RolesJwtAuthenticationConverter();

  @Test
  void shouldConvertRolesClaimToRoleAuthorities() {
    var jwt = Jwt.withTokenValue("token")
        .header("alg", "HS256")
        .subject("1")
        .claim("roles", List.of("USER", "ADMIN"))
        .build();

    var authentication = converter.convert(jwt);

    assertThat(authentication).isNotNull();
    assertThat(authentication.getAuthorities())
        .extracting(GrantedAuthority::getAuthority)
        .contains("ROLE_USER", "ROLE_ADMIN");
  }
}
