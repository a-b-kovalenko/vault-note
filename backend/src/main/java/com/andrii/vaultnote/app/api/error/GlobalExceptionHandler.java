package com.andrii.vaultnote.app.api.error;

import com.andrii.vaultnote.app.exception.AuthenticationFailedException;
import com.andrii.vaultnote.app.exception.EmailVerificationFailedException;
import com.andrii.vaultnote.app.exception.EntityExistsException;
import com.andrii.vaultnote.app.exception.NoteNotFoundException;
import com.andrii.vaultnote.app.exception.NoteVersionConflictException;
import com.andrii.vaultnote.app.exception.PasswordResetFailedException;
import com.andrii.vaultnote.app.exception.RefreshTokenAuthenticationFailedException;
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

  @ExceptionHandler(EmailVerificationFailedException.class)
  public ResponseEntity<ApiErrorResponse> handleEmailVerificationFailedException(
      EmailVerificationFailedException exception) {

    log.warn("Email verification failed: {}", exception.getMessage());

    var response = new ApiErrorResponse("EMAIL_VERIFICATION_FAILED", exception.getMessage());

    return ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .body(response);
  }

  @ExceptionHandler(PasswordResetFailedException.class)
  public ResponseEntity<ApiErrorResponse> handlePasswordResetFailedException(
      PasswordResetFailedException exception) {

    log.warn("Password reset failed");

    var response = new ApiErrorResponse("PASSWORD_RESET_FAILED", exception.getMessage());

    return ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .body(response);
  }

  @ExceptionHandler(AuthenticationFailedException.class)
  public ResponseEntity<ApiErrorResponse> handleAuthenticationFailedException(
      AuthenticationFailedException exception) {

    log.warn("Authentication failed");

    var response = new ApiErrorResponse("AUTHENTICATION_FAILED", exception.getMessage());

    return ResponseEntity
        .status(HttpStatus.UNAUTHORIZED)
        .body(response);
  }

  @ExceptionHandler(RefreshTokenAuthenticationFailedException.class)
  public ResponseEntity<ApiErrorResponse> handleRefreshTokenAuthenticationFailedException(
      RefreshTokenAuthenticationFailedException exception) {

    log.warn("Refresh token authentication failed");

    var response = new ApiErrorResponse(
        "REFRESH_TOKEN_AUTHENTICATION_FAILED",
        exception.getMessage());

    return ResponseEntity
        .status(HttpStatus.UNAUTHORIZED)
        .body(response);
  }

  @ExceptionHandler(NoteNotFoundException.class)
  public ResponseEntity<ApiErrorResponse> handleNoteNotFoundException(
      NoteNotFoundException exception) {

    log.warn("Note not found: {}", exception.getMessage());

    var response = new ApiErrorResponse("NOTE_NOT_FOUND", exception.getMessage());

    return ResponseEntity
        .status(HttpStatus.NOT_FOUND)
        .body(response);
  }

  @ExceptionHandler(NoteVersionConflictException.class)
  public ResponseEntity<ApiErrorResponse> handleNoteVersionConflictException(
      NoteVersionConflictException exception) {

    log.warn("Note version conflict: {}", exception.getMessage());

    var response = new ApiErrorResponse("NOTE_VERSION_CONFLICT", exception.getMessage());

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
