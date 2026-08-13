package com.andrii.vaultnote.app.exception;

public class RefreshTokenAuthenticationFailedException extends RuntimeException {

  public static final String CODE = "REFRESH_TOKEN_AUTHENTICATION_FAILED";

  public RefreshTokenAuthenticationFailedException() {
    super("Invalid or expired refresh token.");
  }
}
