package com.andrii.vaultnote.app.api.error;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ValidationViolation", description = "A field-level validation error.")
public record ValidationViolation(
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String field,
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String code,
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String message) {
}
