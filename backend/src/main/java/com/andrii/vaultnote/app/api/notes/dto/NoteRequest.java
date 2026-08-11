package com.andrii.vaultnote.app.api.notes.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record NoteRequest(
    @NotBlank @Size(max = 200) String title,
    @NotBlank @Size(max = 20_000) String content) {
}
