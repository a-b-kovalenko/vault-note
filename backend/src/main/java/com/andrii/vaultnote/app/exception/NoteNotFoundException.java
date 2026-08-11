package com.andrii.vaultnote.app.exception;

public class NoteNotFoundException extends RuntimeException {

  public NoteNotFoundException(Long noteId) {
    super("Note with id '%s' was not found.".formatted(noteId));
  }
}
