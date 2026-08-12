package com.andrii.vaultnote.app.api.auth.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record LoginRequest(
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Email @Size(max = 320) String email,
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(max = 256) String password) {
}
