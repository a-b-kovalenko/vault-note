package com.andrii.vaultnote.app.api.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record PasswordResetRequest(
  @Schema(
    description = "Email address for the password reset request.",
    requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Email @Size(max = 320) String email) {
}
