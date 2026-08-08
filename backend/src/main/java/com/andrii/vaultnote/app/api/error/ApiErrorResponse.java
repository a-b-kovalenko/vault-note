package com.andrii.vaultnote.app.api.error;

import java.util.List;

public record ApiErrorResponse(
    String code,
    String message,
    List<ValidationViolation> violations) {

  public ApiErrorResponse(String code, String message) {
    this(code, message, List.of());
  }
}
