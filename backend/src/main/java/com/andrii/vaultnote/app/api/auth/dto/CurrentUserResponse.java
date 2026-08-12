package com.andrii.vaultnote.app.api.auth.dto;

import java.util.List;

import lombok.Builder;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record CurrentUserResponse(
  long userId,
  List<String> roles) {
}
