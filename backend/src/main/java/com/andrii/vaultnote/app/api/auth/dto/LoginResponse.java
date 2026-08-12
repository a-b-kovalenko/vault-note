package com.andrii.vaultnote.app.api.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record LoginResponse(
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String accessToken,
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) TokenType tokenType,
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long expiresIn) {
}
