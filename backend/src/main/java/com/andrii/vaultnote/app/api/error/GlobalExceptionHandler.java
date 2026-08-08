package com.andrii.vaultnote.app.api.error;

import com.andrii.vaultnote.app.exception.EntityExistsException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Objects;

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

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiErrorResponse> handleValidationException(
      MethodArgumentNotValidException exception) {

    var violations = exception.getBindingResult()
        .getFieldErrors()
        .stream()
        .map(GlobalExceptionHandler::toViolation)
        .toList();

    var response = new ApiErrorResponse("VALIDATION_FAILED", "Request validation failed", violations);

    return ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .body(response);
  }

  private static ValidationViolation toViolation(FieldError error) {
    return new ValidationViolation(
        toApiFieldName(error.getField()),
        toApiCode(Objects.requireNonNull(error.getCode())),
        error.getDefaultMessage());
  }

  private static String toApiFieldName(String fieldName) {
    return "displayName".equals(fieldName) ? "display_name" : fieldName;
  }

  private static String toApiCode(String validationCode) {
    return switch (validationCode) {
      case "NotBlank" -> "REQUIRED";
      case "Email" -> "INVALID_FORMAT";
      case "Size" -> "INVALID_LENGTH";
      case "PasswordPolicy" -> "PASSWORD_POLICY";
      default -> "INVALID_VALUE";
    };
  }
}
