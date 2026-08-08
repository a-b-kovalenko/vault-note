package com.andrii.vaultnote.app.api.error;

import com.andrii.vaultnote.app.exception.EntityExistsException;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(EntityExistsException.class)
  public ResponseEntity<ApiErrorResponse> handleEntityExistsException(
      EntityExistsException exception) {

    log.warn("Entity already exists: {}", exception.getMessage());

    var response = new ApiErrorResponse("ENTITY_ALREADY_EXISTS", exception.getMessage());

    return ResponseEntity
        .status(HttpStatus.CONFLICT)
        .body(response);
  }
}
