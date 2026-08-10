package com.andrii.vaultnote.app.security;

import static java.util.Objects.nonNull;

import com.andrii.vaultnote.users.domain.UserRole;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class JwtRolesValidator implements OAuth2TokenValidator<Jwt> {

  private static final String ROLES_CLAIM = "roles";
  private static final Set<String> KNOWN_ROLES = Arrays.stream(UserRole.values())
      .map(UserRole::name)
      .collect(Collectors.toUnmodifiableSet());
  private static final OAuth2Error INVALID_ROLES = new OAuth2Error(
      OAuth2ErrorCodes.INVALID_TOKEN,
      "JWT contains unknown or missing roles",
      null);

  @Override
  public OAuth2TokenValidatorResult validate(Jwt token) {
    var roles = token.getClaimAsStringList(ROLES_CLAIM);
    var valid = nonNull(roles)
        && !roles.isEmpty()
        && KNOWN_ROLES.containsAll(roles);

    return valid
        ? OAuth2TokenValidatorResult.success()
        : OAuth2TokenValidatorResult.failure(INVALID_ROLES);
  }
}
