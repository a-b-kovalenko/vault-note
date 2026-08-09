package com.andrii.vaultnote.users.domain;

public enum UserRole {

  USER((short) 1), ADMIN((short) 2);

  private final short code;

  UserRole(short code) {
    this.code = code;
  }

  public short code() {
    return code;
  }

  public static UserRole fromCode(short code) {
    return switch (code) {
      case 1 -> USER;
      case 2 -> ADMIN;
      default -> throw new IllegalArgumentException("Unknown user role code: " + code);
    };
  }
}
