package com.andrii.vaultnote.app.api.notes.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.time.Instant;
import lombok.Builder;

@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record NoteInfoDto(
  Long id,
  String title,
  String content,
  long version,
  Instant createdAt,
  Instant updatedAt) {
}
