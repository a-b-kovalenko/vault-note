package com.andrii.vaultnote.app.exception;

public class PasswordResetFailedException extends RuntimeException {

  public PasswordResetFailedException() {
    super("Password reset link is invalid or has expired.");
  }
}
