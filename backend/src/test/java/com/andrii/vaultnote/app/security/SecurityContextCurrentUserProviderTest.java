package com.andrii.vaultnote.app.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

class SecurityContextCurrentUserProviderTest {

  private final CurrentUserProvider provider = new SecurityContextCurrentUserProvider();

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void shouldReturnCurrentUserIdFromAuthenticationName() {
    var authentication = UsernamePasswordAuthenticationToken.authenticated(
        "42",
        null,
        List.of());
    SecurityContextHolder.getContext().setAuthentication(authentication);

    var currentUserId = provider.currentUserId();

    assertThat(currentUserId).isEqualTo(42L);
  }

  @Test
  void shouldRejectMissingAuthentication() {
    assertThatExceptionOfType(AuthenticationCredentialsNotFoundException.class)
        .isThrownBy(provider::currentUserId)
        .withMessage("An authenticated user is required");
  }

  @Test
  void shouldRejectAnonymousAuthentication() {
    var authentication = new AnonymousAuthenticationToken(
        "key",
        "anonymousUser",
        AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));
    SecurityContextHolder.getContext().setAuthentication(authentication);

    assertThatExceptionOfType(AuthenticationCredentialsNotFoundException.class)
        .isThrownBy(provider::currentUserId)
        .withMessage("An authenticated user is required");
  }

  @Test
  void shouldRejectNonNumericAuthenticationName() {
    var authentication = UsernamePasswordAuthenticationToken.authenticated(
        "andrii@example.com",
        null,
        List.of());
    SecurityContextHolder.getContext().setAuthentication(authentication);

    assertThatExceptionOfType(AuthenticationCredentialsNotFoundException.class)
        .isThrownBy(provider::currentUserId)
        .withMessage("Authenticated user subject must be numeric");
  }
}
