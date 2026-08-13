package com.andrii.vaultnote.app.exception;

public class EmailVerificationFailedException extends RuntimeException {

  public static final String CODE = "EMAIL_VERIFICATION_FAILED";

  public EmailVerificationFailedException() {
    super("Email verification link is invalid or has expired.");
  }
}
