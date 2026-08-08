package com.andrii.vaultnote.app.api.auth.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@Builder(toBuilder = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record RegisterUserRequest(
    @NotNull String email,
    @NotNull String displayName,
    @NotNull String password) {
}
