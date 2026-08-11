package com.andrii.vaultnote.app.service.impl;

import com.andrii.vaultnote.app.security.CurrentUserProvider;
import com.andrii.vaultnote.app.api.notes.dto.NoteInfoDto;
import com.andrii.vaultnote.app.exception.NoteNotFoundException;
import com.andrii.vaultnote.app.exception.NoteVersionConflictException;
import com.andrii.vaultnote.app.mapper.NoteMapper;
import com.andrii.vaultnote.app.service.NoteService;
import com.andrii.vaultnote.notes.infrastructure.persistence.entity.NoteEntity;
import com.andrii.vaultnote.notes.infrastructure.persistence.repository.NoteJpaRepository;
import com.andrii.vaultnote.users.infrastructure.persistence.repository.UserJpaRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NoteServiceImpl implements NoteService {

  NoteJpaRepository noteRepository;
  UserJpaRepository userRepository;
  CurrentUserProvider currentUserProvider;
  NoteMapper noteMapper;

  @Override
  @Transactional(readOnly = true)
  public Page<NoteInfoDto> getNotes(Pageable pageable) {
    var ownerId = currentUserProvider.currentUserId();
    log.info("Getting notes: ownerId={}, page={}, size={}, sort={}",
        ownerId, pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());
    return noteRepository.findByOwner_Id(ownerId, pageable)
        .map(noteMapper::toNoteInfoDto);
  }

  @Override
  @Transactional(readOnly = true)
  public NoteInfoDto getNote(Long noteId) {
    var ownerId = currentUserProvider.currentUserId();
    log.info("Getting note: noteId={}, ownerId={}", noteId, ownerId);
    return noteRepository.findByIdAndOwner_Id(noteId, ownerId)
        .map(noteMapper::toNoteInfoDto)
        .orElseThrow(() -> new NoteNotFoundException(noteId));
  }

  @Override
  @Transactional
  public NoteInfoDto createNote(String title, String content) {
    var ownerId = currentUserProvider.currentUserId();
    log.info("Creating note for ownerId={}", ownerId);
    var owner = userRepository.getReferenceById(ownerId);
    var note = NoteEntity.builder()
        .owner(owner)
        .title(title)
        .content(content)
        .build();
    return noteMapper.toNoteInfoDto(noteRepository.save(note));
  }

  @Override
  @Transactional
  public NoteInfoDto updateNote(Long noteId, String title, String content, long expectedVersion) {
    var ownerId = currentUserProvider.currentUserId();
    log.info("Updating note: noteId={}, ownerId={}, expectedVersion={}",
        noteId, ownerId, expectedVersion);
    return noteRepository.findByIdAndOwner_Id(noteId, ownerId)
        .map(note -> updateNote(note, title, content, expectedVersion))
        .map(noteRepository::saveAndFlush)
        .map(noteMapper::toNoteInfoDto)
        .orElseThrow(() -> new NoteNotFoundException(noteId));
  }

  @Override
  @Transactional
  public void deleteNote(Long noteId) {
    var ownerId = currentUserProvider.currentUserId();
    log.info("Deleting note: noteId={}, ownerId={}", noteId, ownerId);
    var note = noteRepository.findByIdAndOwner_Id(noteId, ownerId)
        .orElseThrow(() -> new NoteNotFoundException(noteId));
    noteRepository.delete(note);
  }

  private NoteEntity updateNote(
      NoteEntity note, String title, String content, long expectedVersion) {
    if (note.getVersion() != expectedVersion) {
      throw new NoteVersionConflictException(note.getId(), expectedVersion, note.getVersion());
    }
    note.update(title, content);
    return note;
  }

}
