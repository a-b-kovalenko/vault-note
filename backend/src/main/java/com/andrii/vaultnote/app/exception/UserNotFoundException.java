package com.andrii.vaultnote.app.exception;

public class UserNotFoundException extends RuntimeException {

  public static final String CODE = "USER_NOT_FOUND";

  public UserNotFoundException(Long userId) {
    super("User with id '%s' was not found.".formatted(userId));
  }
}
