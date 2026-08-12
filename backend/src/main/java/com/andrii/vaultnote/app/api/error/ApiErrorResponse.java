package com.andrii.vaultnote.app.api.error;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(name = "ApiErrorResponse", description = "Stable API error response.")
public record ApiErrorResponse(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String code,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String message,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<ValidationViolation> violations) {

  public ApiErrorResponse(String code, String message) {
    this(code, message, List.of());
  }
}
