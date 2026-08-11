package com.andrii.vaultnote.app.api.notes;

public record NoteEtag(long version) {

  public static NoteEtag parse(String ifMatch) {
    var value = ifMatch.trim();
    if (value.length() < 3 || !value.startsWith("\"") || !value.endsWith("\"")) {
      throw new IllegalArgumentException("If-Match must contain a quoted note version.");
    }

    try {
      var version = Long.parseLong(value.substring(1, value.length() - 1));
      if (version < 0) {
        throw new IllegalArgumentException("If-Match must contain a non-negative note version.");
      }
      return new NoteEtag(version);
    } catch (NumberFormatException exception) {
      throw new IllegalArgumentException("If-Match must contain a numeric note version.", exception);
    }
  }

  public String headerValue() {
    return "\"%d\"".formatted(version);
  }
}
