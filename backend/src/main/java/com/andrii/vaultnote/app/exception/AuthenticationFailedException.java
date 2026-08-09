package com.andrii.vaultnote.app.exception;

public class AuthenticationFailedException extends RuntimeException {
  public AuthenticationFailedException() {
    super("Invalid email or password.");
  }
}
