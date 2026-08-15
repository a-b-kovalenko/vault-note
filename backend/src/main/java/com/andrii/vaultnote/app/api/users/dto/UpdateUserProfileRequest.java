package com.andrii.vaultnote.app.api.users.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import com.andrii.vaultnote.users.domain.DisplayNameRules;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@Builder(toBuilder = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record UpdateUserProfileRequest(
  @NotBlank @Size(max = DisplayNameRules.MAX_LENGTH) String displayName) {
}
