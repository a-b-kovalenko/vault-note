package com.andrii.vaultnote.app.api.users.dto;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;
import lombok.Builder;

@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record UserInfoDto(
  Long id,
  String email,
  String displayName) {
}
