package com.andrii.vaultnote.app.api.auth.dto;

import com.andrii.vaultnote.app.api.auth.policy.PasswordPolicy;
import com.andrii.vaultnote.users.domain.DisplayNameRules;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@Builder(toBuilder = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record RegisterUserRequest(
  @NotBlank @Email @Size(max = 320) String email,

  @NotBlank @Size(max = DisplayNameRules.MAX_LENGTH) String displayName,

  @NotBlank @Size(min = 12, max = 256) @PasswordPolicy String password) {
}
