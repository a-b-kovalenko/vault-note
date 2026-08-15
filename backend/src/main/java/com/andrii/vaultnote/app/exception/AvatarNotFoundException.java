package com.andrii.vaultnote.app.exception;

public class AvatarNotFoundException extends RuntimeException {

  public static final String CODE = "AVATAR_NOT_FOUND";

  public AvatarNotFoundException(Long userId) {
    super("Avatar for user with id '%s' was not found.".formatted(userId));
  }
}
