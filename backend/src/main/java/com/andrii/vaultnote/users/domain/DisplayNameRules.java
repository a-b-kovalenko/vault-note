package com.andrii.vaultnote.users.domain;

public final class DisplayNameRules {

  public static final int MAX_LENGTH = 100;

  private DisplayNameRules() {
  }

  public static void validate(String displayName) {
    if (displayName == null || displayName.isBlank()) {
      throw new IllegalArgumentException("Display name must not be blank.");
    }
    if (displayName.length() > MAX_LENGTH) {
      throw new IllegalArgumentException(
        "Display name must not exceed %d characters.".formatted(MAX_LENGTH));
    }
  }
}
