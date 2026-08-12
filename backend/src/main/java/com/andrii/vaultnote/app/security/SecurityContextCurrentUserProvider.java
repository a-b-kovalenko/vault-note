package com.andrii.vaultnote.app.security;

import static java.util.Objects.isNull;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class SecurityContextCurrentUserProvider implements CurrentUserProvider {

  private static final String AUTHENTICATED_USER_REQUIRED = "An authenticated user is required";
  private static final String NUMERIC_SUBJECT_REQUIRED = "Authenticated user subject must be numeric";

  @Override
  public long currentUserId() {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    if (isNull(authentication)
      || !authentication.isAuthenticated()
      || authentication instanceof AnonymousAuthenticationToken) {
      throw new AuthenticationCredentialsNotFoundException(AUTHENTICATED_USER_REQUIRED);
    }

    return parseUserId(authentication);
  }

  private long parseUserId(Authentication authentication) {
    try {
      return Long.parseLong(authentication.getName());
    } catch (NumberFormatException exception) {
      throw new AuthenticationCredentialsNotFoundException(NUMERIC_SUBJECT_REQUIRED, exception);
    }
  }
}
