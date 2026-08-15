package com.andrii.vaultnote.app.exception;

public class AvatarValidationException extends RuntimeException {

  public static final String CODE = "INVALID_AVATAR";

  public AvatarValidationException(String message) {
    super(message);
  }

  public AvatarValidationException(String message, Throwable cause) {
    super(message, cause);
  }
}
