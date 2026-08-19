package com.andrii.vaultnote.app.security.csrf;

import java.util.Objects;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.DefaultCsrfToken;

/**
 * Provides CSRF tokens that are verified from the request header without
 * storing the expected token in a server-side session or cookie.
 */
public final class StatelessCsrfTokenRepository implements CsrfTokenRepository {

  private static final String HEADER_NAME = "X-XSRF-TOKEN";
  private static final String PARAMETER_NAME = "_csrf";

  private final StatelessCsrfTokenService tokenService;

  public StatelessCsrfTokenRepository(StatelessCsrfTokenService tokenService) {
    this.tokenService = Objects.requireNonNull(tokenService, "tokenService");
  }

  @Override
  public CsrfToken generateToken(HttpServletRequest request) {
    return new DefaultCsrfToken(HEADER_NAME, PARAMETER_NAME, tokenService.generateToken());
  }

  @Override
  public void saveToken(CsrfToken token, HttpServletRequest request, HttpServletResponse response) {
    // The token is self-verifying and is intentionally not persisted anywhere.
  }

  @Override
  public CsrfToken loadToken(HttpServletRequest request) {
    if (request == null) {
      return null;
    }

    var token = request.getHeader(HEADER_NAME);
    if (!tokenService.isValid(token)) {
      return null;
    }
    return new DefaultCsrfToken(HEADER_NAME, PARAMETER_NAME, token);
  }
}
