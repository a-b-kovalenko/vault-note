package com.andrii.vaultnote.app.exception;

public class PasswordResetFailedException extends RuntimeException {

  public static final String CODE = "PASSWORD_RESET_FAILED";

  public PasswordResetFailedException() {
    super("Password reset link is invalid or has expired.");
  }
}
