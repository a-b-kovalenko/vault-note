package com.andrii.vaultnote.app.api.auth.dto;

import com.andrii.vaultnote.app.api.auth.policy.PasswordPolicy;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record PasswordResetConfirmRequest(
  @Schema(
    description = "Single-use password reset token from the email link.",
    requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank String token,

  @Schema(description = "New local password.", requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(
    min = 12,
    max = 256) @PasswordPolicy String newPassword) {
}
