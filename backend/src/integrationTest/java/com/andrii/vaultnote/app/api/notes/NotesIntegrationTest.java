package com.andrii.vaultnote.app.api.notes;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import com.andrii.vaultnote.app.api.error.ApiErrorResponse;
import com.andrii.vaultnote.app.api.notes.dto.NoteRequest;
import com.andrii.vaultnote.app.security.AccessTokenGenerator;
import com.andrii.vaultnote.notes.infrastructure.persistence.entity.NoteEntity;
import com.andrii.vaultnote.notes.infrastructure.persistence.repository.NoteJpaRepository;
import com.andrii.vaultnote.support.AbstractBaseIntegrationTest;
import com.andrii.vaultnote.users.domain.UserRole;
import com.andrii.vaultnote.users.infrastructure.persistence.entity.UserEntity;
import com.andrii.vaultnote.users.infrastructure.persistence.repository.UserJpaRepository;
import com.github.database.rider.core.api.dataset.DataSet;
import io.restassured.http.ContentType;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import java.util.EnumSet;

@DataSet(value = "auth-baseline.yml", skipCleaningFor = {"databasechangelog", "databasechangeloglock"})
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
class NotesIntegrationTest extends AbstractBaseIntegrationTest {

  private static final String NOTES_ENDPOINT = "/api/v1/notes";

  UserJpaRepository userRepository;
  NoteJpaRepository noteRepository;
  AccessTokenGenerator accessTokenGenerator;

  @Autowired
  NotesIntegrationTest(
      UserJpaRepository userRepository,
      NoteJpaRepository noteRepository,
      AccessTokenGenerator accessTokenGenerator) {
    this.userRepository = userRepository;
    this.noteRepository = noteRepository;
    this.accessTokenGenerator = accessTokenGenerator;
  }

  /**
   * Rejects a stale note update after a newer version has already been persisted.
   *
   * <p>
   * The first update must increment the JPA version and expose it as an ETag. A
   * second update using the old ETag must return {@code 409 Conflict} and must
   * not overwrite the newer database state.
   */
  @Test
  void shouldRejectStaleNoteUpdate() {
    var owner = saveUser();
    var accessToken = accessTokenGenerator.generate(owner).rawValue();
    var createRequest = NoteRequest.builder()
        .title("Initial title")
        .content("Initial content")
        .build();

    var createdResponse = givenWithCsrf()
        .auth()
        .oauth2(accessToken)
        .contentType(ContentType.JSON)
        .body(createRequest)
        .when()
        .post(NOTES_ENDPOINT)
        .then()
        .statusCode(HttpStatus.CREATED.value())
        .extract()
        .response();
    var noteId = createdResponse.jsonPath().getLong("id");

    var currentResponse = given()
        .port(port)
        .auth()
        .oauth2(accessToken)
        .when()
        .get(NOTES_ENDPOINT + "/" + noteId)
        .then()
        .statusCode(HttpStatus.OK.value())
        .extract()
        .response();
    var initialEtag = currentResponse.getHeader(HttpHeaders.ETAG);
    assertThat(initialEtag).isEqualTo("\"0\"");

    var updateRequest = NoteRequest.builder()
        .title("Updated title")
        .content("Updated content")
        .build();
    var updatedResponse = givenWithCsrf()
        .auth()
        .oauth2(accessToken)
        .contentType(ContentType.JSON)
        .header(HttpHeaders.IF_MATCH, initialEtag)
        .body(updateRequest)
        .when()
        .put(NOTES_ENDPOINT + "/" + noteId)
        .then()
        .statusCode(HttpStatus.OK.value())
        .extract()
        .response();

    assertThat(updatedResponse.getHeader(HttpHeaders.ETAG)).isEqualTo("\"1\"");

    assertThat(updatedResponse.jsonPath())
        .extracting(
            json -> json.getLong("id"),
            json -> json.getString("title"),
            json -> json.getString("content"),
            json -> json.getLong("version"))
        .containsExactly(noteId, "Updated title", "Updated content", 1L);

    var conflictResponse = givenWithCsrf()
        .auth()
        .oauth2(accessToken)
        .contentType(ContentType.JSON)
        .header(HttpHeaders.IF_MATCH, initialEtag)
        .body(NoteRequest.builder()
            .title("Stale title")
            .content("Stale content")
            .build())
        .when()
        .put(NOTES_ENDPOINT + "/" + noteId)
        .then()
        .statusCode(HttpStatus.CONFLICT.value())
        .extract()
        .as(ApiErrorResponse.class);

    assertThat(conflictResponse.code()).isEqualTo("NOTE_VERSION_CONFLICT");

    var savedNote = noteRepository.findById(noteId).orElseThrow();
    assertThat(savedNote)
        .extracting(NoteEntity::getTitle, NoteEntity::getContent, NoteEntity::getVersion)
        .containsExactly("Updated title", "Updated content", 1L);
  }

  private UserEntity saveUser() {
    return userRepository.saveAndFlush(UserEntity.builder()
        .email("notes-owner@example.com")
        .displayName("Notes Owner")
        .passwordHash("password-hash")
        .emailVerified(true)
        .roles(EnumSet.of(UserRole.USER))
        .build());
  }
}
