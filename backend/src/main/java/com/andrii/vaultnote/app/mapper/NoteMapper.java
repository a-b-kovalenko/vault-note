package com.andrii.vaultnote.app.mapper;

import com.andrii.vaultnote.app.api.notes.dto.NoteInfoDto;
import com.andrii.vaultnote.notes.infrastructure.persistence.entity.NoteEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface NoteMapper {

  NoteInfoDto toNoteInfoDto(NoteEntity noteEntity);
}
