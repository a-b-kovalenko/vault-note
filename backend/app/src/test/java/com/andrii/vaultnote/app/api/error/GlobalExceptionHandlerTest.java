package com.andrii.vaultnote.app.api.error;

import com.andrii.vaultnote.app.exception.EntityExistsException;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

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
        .extracting(ApiErrorResponse::code)
        .isEqualTo("ENTITY_ALREADY_EXISTS");
    assertThat(response.getBody().message())
        .isEqualTo("UserEntity with email 'new@email.com' already exists.");
  }
}
