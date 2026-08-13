package com.andrii.vaultnote.app.exception;

public class AuthenticationFailedException extends RuntimeException {

  public static final String CODE = "AUTHENTICATION_FAILED";

  public AuthenticationFailedException() {
    super("Invalid email or password.");
  }
}
