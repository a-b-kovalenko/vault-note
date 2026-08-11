package com.andrii.vaultnote.app.exception;

public class RefreshTokenAuthenticationFailedException extends RuntimeException {

  public RefreshTokenAuthenticationFailedException() {
    super("Invalid or expired refresh token.");
  }
}
