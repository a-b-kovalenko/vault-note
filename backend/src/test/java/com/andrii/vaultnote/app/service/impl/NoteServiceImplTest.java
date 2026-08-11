package com.andrii.vaultnote.app.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.andrii.vaultnote.app.api.notes.dto.NoteInfoDto;
import com.andrii.vaultnote.app.mapper.NoteMapper;
import com.andrii.vaultnote.app.security.CurrentUserProvider;
import com.andrii.vaultnote.app.exception.NoteNotFoundException;
import com.andrii.vaultnote.notes.infrastructure.persistence.entity.NoteEntity;
import com.andrii.vaultnote.notes.infrastructure.persistence.repository.NoteJpaRepository;
import com.andrii.vaultnote.users.infrastructure.persistence.entity.UserEntity;
import com.andrii.vaultnote.users.infrastructure.persistence.repository.UserJpaRepository;
import java.util.List;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
@FieldDefaults(level = AccessLevel.PRIVATE)
class NoteServiceImplTest {

  private static final long USER_ID = 3L;
  private static final long NOTE_ID = 11L;
  private static final String TITLE = "My note";
  private static final String CONTENT = "# Content";

  @Mock
  NoteJpaRepository noteRepository;

  @Mock
  UserJpaRepository userRepository;

  @Mock
  CurrentUserProvider currentUserProvider;

  @Spy
  NoteMapper noteMapper = Mappers.getMapper(NoteMapper.class);

  @InjectMocks
  NoteServiceImpl noteService;

  @Test
  void shouldGetCurrentUserNotes() {
    var pageable = PageRequest.of(0, 20);
    var note = note(NOTE_ID, TITLE, CONTENT);
    var notes = new PageImpl<>(List.of(note), pageable, 1);

    when(currentUserProvider.currentUserId()).thenReturn(USER_ID);
    when(noteRepository.findByOwner_Id(USER_ID, pageable)).thenReturn(notes);

    var result = noteService.getNotes(pageable);

    assertThat(result.getContent())
        .extracting(NoteInfoDto::id, NoteInfoDto::title, NoteInfoDto::content)
        .containsExactly(tuple(NOTE_ID, TITLE, CONTENT));
    verify(noteMapper).toNoteInfoDto(note);
  }

  @Test
  void shouldGetOwnedNote() {
    var note = note(NOTE_ID, TITLE, CONTENT);

    when(currentUserProvider.currentUserId()).thenReturn(USER_ID);
    when(noteRepository.findByIdAndOwner_Id(NOTE_ID, USER_ID)).thenReturn(Optional.of(note));

    var result = noteService.getNote(NOTE_ID);

    assertThat(result)
        .extracting(NoteInfoDto::id, NoteInfoDto::title, NoteInfoDto::content)
        .containsExactly(NOTE_ID, TITLE, CONTENT);
    verify(noteMapper).toNoteInfoDto(note);
  }

  @Test
  void shouldThrowWhenGettingMissingNote() {
    when(currentUserProvider.currentUserId()).thenReturn(USER_ID);
    when(noteRepository.findByIdAndOwner_Id(NOTE_ID, USER_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> noteService.getNote(NOTE_ID))
        .isInstanceOf(NoteNotFoundException.class)
        .hasMessage("Note with id '11' was not found.");
  }

  @Test
  void shouldCreateNoteForCurrentUser() {
    var owner = UserEntity.builder().id(USER_ID).build();

    when(currentUserProvider.currentUserId()).thenReturn(USER_ID);
    when(userRepository.getReferenceById(USER_ID)).thenReturn(owner);
    when(noteRepository.save(any(NoteEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var result = noteService.createNote(TITLE, CONTENT);

    assertThat(result)
        .extracting(NoteInfoDto::title, NoteInfoDto::content)
        .containsExactly(TITLE, CONTENT);
    var noteCaptor = ArgumentCaptor.forClass(NoteEntity.class);
    verify(noteRepository).save(noteCaptor.capture());

    assertThat(noteCaptor.getValue().getOwner()).isSameAs(owner);
    verify(noteMapper).toNoteInfoDto(noteCaptor.getValue());
  }

  @Test
  void shouldUpdateOwnedNote() {
    var note = note(NOTE_ID, "Old title", "Old content");
    var updatedTitle = "Updated title";
    var updatedContent = "Updated content";

    when(currentUserProvider.currentUserId()).thenReturn(USER_ID);
    when(noteRepository.findByIdAndOwner_Id(NOTE_ID, USER_ID)).thenReturn(Optional.of(note));
    when(noteRepository.saveAndFlush(any(NoteEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var result = noteService.updateNote(NOTE_ID, updatedTitle, updatedContent, 0L);

    assertThat(result)
        .extracting(NoteInfoDto::id, NoteInfoDto::title, NoteInfoDto::content)
        .containsExactly(NOTE_ID, updatedTitle, updatedContent);
    verify(noteRepository).saveAndFlush(any(NoteEntity.class));
    verify(noteMapper).toNoteInfoDto(any(NoteEntity.class));
  }

  @Test
  void shouldThrowWhenUpdatingMissingNote() {
    when(currentUserProvider.currentUserId()).thenReturn(USER_ID);
    when(noteRepository.findByIdAndOwner_Id(NOTE_ID, USER_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> noteService.updateNote(NOTE_ID, TITLE, CONTENT, 0L))
        .isInstanceOf(NoteNotFoundException.class);
    verify(noteRepository, never()).save(any(NoteEntity.class));
    verify(noteMapper, never()).toNoteInfoDto(any(NoteEntity.class));
  }

  @Test
  void shouldDeleteOwnedNote() {
    var note = note(NOTE_ID, TITLE, CONTENT);

    when(currentUserProvider.currentUserId()).thenReturn(USER_ID);
    when(noteRepository.findByIdAndOwner_Id(NOTE_ID, USER_ID)).thenReturn(Optional.of(note));

    noteService.deleteNote(NOTE_ID);

    verify(noteRepository).delete(note);
  }

  @Test
  void shouldThrowWhenDeletingMissingNote() {
    when(currentUserProvider.currentUserId()).thenReturn(USER_ID);
    when(noteRepository.findByIdAndOwner_Id(NOTE_ID, USER_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> noteService.deleteNote(NOTE_ID))
        .isInstanceOf(NoteNotFoundException.class);
    verify(noteRepository, never()).delete(any(NoteEntity.class));
  }

  private static NoteEntity note(long id, String title, String content) {
    return NoteEntity.builder()
        .id(id)
        .title(title)
        .content(content)
        .build();
  }
}
