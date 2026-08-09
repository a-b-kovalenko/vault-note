package com.andrii.vaultnote.app.exception;

public class EmailVerificationFailedException extends RuntimeException {
  public EmailVerificationFailedException() {
    super("Email verification link is invalid or has expired.");
  }
}
