package com.andrii.vaultnote.app.exception;

public class NoteVersionConflictException extends RuntimeException {

  public NoteVersionConflictException(Long noteId, long expectedVersion, long actualVersion) {
    super("Note with id '%s' has version '%s', but version '%s' was expected."
      .formatted(noteId, actualVersion, expectedVersion));
  }
}
