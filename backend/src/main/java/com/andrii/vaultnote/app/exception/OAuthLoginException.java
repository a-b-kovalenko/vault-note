package com.andrii.vaultnote.app.exception;

public class OAuthLoginException extends RuntimeException {

  public static final String CODE = "OAUTH_LOGIN_FAILED";

  public OAuthLoginException() {
    super("Unable to complete OAuth sign-in.");
  }
}
