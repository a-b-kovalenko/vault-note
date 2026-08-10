package com.andrii.vaultnote.app.security;

import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.stereotype.Component;

@Component
public class RolesJwtAuthenticationConverter extends JwtAuthenticationConverter {

  private static final String ROLES_CLAIM = "roles";
  private static final String ROLE_PREFIX = "ROLE_";

  public RolesJwtAuthenticationConverter() {
    var authoritiesConverter = new JwtGrantedAuthoritiesConverter();
    authoritiesConverter.setAuthoritiesClaimName(ROLES_CLAIM);
    authoritiesConverter.setAuthorityPrefix(ROLE_PREFIX);
    setJwtGrantedAuthoritiesConverter(authoritiesConverter);
  }
}
