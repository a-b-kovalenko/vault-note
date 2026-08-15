package com.andrii.vaultnote.app.api.users.dto;

import java.util.List;
import lombok.Builder;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record UserProfileDto(
  Long id,
  String email,
  String displayName,
  boolean emailVerified,
  List<String> roles) {
}
