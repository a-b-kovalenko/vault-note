package com.andrii.vaultnote.app.api.error;

import com.andrii.vaultnote.app.exception.EntityExistsException;
import com.andrii.vaultnote.app.exception.EmailVerificationFailedException;
import com.andrii.vaultnote.app.exception.NoteNotFoundException;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.core.MethodParameter;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@FieldDefaults(level = AccessLevel.PRIVATE)
class GlobalExceptionHandlerTest {

  final GlobalExceptionHandler handler = new GlobalExceptionHandler();

  @Test
  void shouldHandleEntityExistsException() {
    var exception = new EntityExistsException("UserEntity", "email", "new@email.com");

    var response = handler.handleEntityExistsException(exception);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(response.getBody())
        .isNotNull()
        .extracting(ApiErrorResponse::code, ApiErrorResponse::message)
        .containsExactly(
            "ENTITY_ALREADY_EXISTS",
            "UserEntity with email 'new@email.com' already exists.");
  }

  @Test
  void shouldHandleEmailVerificationFailedException() {
    var exception = new EmailVerificationFailedException();

    var response = handler.handleEmailVerificationFailedException(exception);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody())
        .isNotNull()
        .extracting(ApiErrorResponse::code, ApiErrorResponse::message)
        .containsExactly(
            "EMAIL_VERIFICATION_FAILED",
            "Email verification link is invalid or has expired.");
  }

  @Test
  void shouldHandleNoteNotFoundException() {
    var exception = new NoteNotFoundException(11L);

    var response = handler.handleNoteNotFoundException(exception);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getBody())
        .isNotNull()
        .extracting(ApiErrorResponse::code, ApiErrorResponse::message)
        .containsExactly("NOTE_NOT_FOUND", "Note with id '11' was not found.");
  }

  @Test
  void shouldHandleValidationExceptionWithFieldLevelViolations() throws NoSuchMethodException {
    var bindingResult = new BeanPropertyBindingResult(new Object(), "request");
    bindingResult.addError(fieldError("email", "Email", "must be a valid email"));
    bindingResult.addError(fieldError("displayName", "NotBlank", "must not be blank"));
    bindingResult.addError(fieldError("password", "Size", "size must be between 12 and 256"));
    bindingResult.addError(fieldError("password", "PasswordPolicy", "password policy failed"));
    bindingResult.addError(fieldError("unknown", "Unknown", "invalid value"));

    var method = ValidationTestController.class.getDeclaredMethod("handle", Object.class);
    var exception = new MethodArgumentNotValidException(
        new MethodParameter(method, 0),
        bindingResult);

    var response = handler.handleValidationException(exception);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody())
        .isNotNull()
        .extracting(ApiErrorResponse::code, ApiErrorResponse::message)
        .containsExactly("VALIDATION_FAILED", "Request validation failed");
    assertThat(response.getBody().violations())
        .extracting(ValidationViolation::field, ValidationViolation::code)
        .containsExactly(
            tuple("email", "INVALID_FORMAT"),
            tuple("display_name", "REQUIRED"),
            tuple("password", "INVALID_LENGTH"),
            tuple("password", "PASSWORD_POLICY"),
            tuple("unknown", "INVALID_VALUE"));
  }

  private static FieldError fieldError(String field, String code, String message) {
    return new FieldError("request", field, null, false, new String[]{code}, null, message);
  }

  private static final class ValidationTestController {

    @SuppressWarnings("unused")
    void handle(Object request) {
    }
  }
}
