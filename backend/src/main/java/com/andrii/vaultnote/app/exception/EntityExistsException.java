package com.andrii.vaultnote.app.exception;

public class EntityExistsException extends RuntimeException {
  public EntityExistsException(String entityName, String searchedByField, Object fieldValue) {
    super("%s with %s '%s' already exists.".formatted(entityName, searchedByField, fieldValue));
  }
}
