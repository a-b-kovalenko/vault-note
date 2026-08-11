package com.andrii.vaultnote.app.service;

import com.andrii.vaultnote.app.api.notes.dto.NoteInfoDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NoteService {

  Page<NoteInfoDto> getNotes(Pageable pageable);

  NoteInfoDto getNote(Long noteId);

  NoteInfoDto createNote(String title, String content);

  NoteInfoDto updateNote(Long noteId, String title, String content);

  void deleteNote(Long noteId);
}
