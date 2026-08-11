package com.andrii.vaultnote.app.api.notes;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import com.andrii.vaultnote.app.api.error.ApiErrorResponse;
import com.andrii.vaultnote.app.api.error.ValidationViolation;
import com.andrii.vaultnote.app.api.notes.dto.NoteInfoDto;
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
import java.util.EnumSet;
import java.util.List;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

@DataSet(value = "auth-baseline.yml", skipCleaningFor = {"databasechangelog", "databasechangeloglock"})
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
class NotesIntegrationTest extends AbstractBaseIntegrationTest {

  private static final String NOTES_ENDPOINT = "/api/v1/notes";
  private static final String OWNER_EMAIL = "existing@example.com";
  private static final String OTHER_OWNER_EMAIL = "verification@example.com";

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

  /**
   * Creates a note for the authenticated user and persists its owner relation.
   *
   * <p>
   * The endpoint must return {@code 201 Created}, expose a response DTO, and
   * persist the note with the user from the access token as its owner.
   */
  @Test
  void shouldCreateOwnedNoteAgainstPostgres() {
    var owner = findUser(OWNER_EMAIL);
    var accessToken = accessTokenGenerator.generate(owner).rawValue();
    var request = NoteRequest.builder()
        .title("Integration note")
        .content("# Created through the API")
        .build();

    var response = givenWithCsrf()
        .port(port)
        .auth()
        .oauth2(accessToken)
        .contentType(ContentType.JSON)
        .body(request)
        .when()
        .post(NOTES_ENDPOINT)
        .then()
        .statusCode(HttpStatus.CREATED.value())
        .extract()
        .as(NoteInfoDto.class);

    assertThat(response.id()).isPositive();
    assertThat(response.title()).isEqualTo(request.title());
    assertThat(response.content()).isEqualTo(request.content());

    var savedNote = noteRepository.findById(response.id()).orElseThrow();
    assertThat(savedNote)
        .extracting(
            note -> note.getOwner().getId(),
            NoteEntity::getTitle,
            NoteEntity::getContent)
        .containsExactly(owner.getId(), request.title(), request.content());
  }

  /**
   * Returns only notes owned by the authenticated user and applies the requested
   * pagination and sorting.
   */
  @Test
  void shouldReturnPaginatedOwnedNotesAgainstPostgres() {
    var owner = findUser(OWNER_EMAIL);
    var accessToken = accessTokenGenerator.generate(owner).rawValue();
    var firstNote = saveNote(owner, "A note", "First content");
    saveNote(owner, "B note", "Second content");
    saveNote(findUser(OTHER_OWNER_EMAIL), "Other note", "Other content");

    var response = given()
        .port(port)
        .auth()
        .oauth2(accessToken)
        .queryParam("page", 0)
        .queryParam("size", 1)
        .queryParam("sort", "title,asc")
        .when()
        .get(NOTES_ENDPOINT)
        .then()
        .statusCode(HttpStatus.OK.value())
        .extract()
        .response();

    var page = response.as(NotePageResponse.class);
    assertThat(page)
        .extracting(
            NotePageResponse::number,
            NotePageResponse::size,
            NotePageResponse::numberOfElements,
            NotePageResponse::totalElements)
        .containsExactly(0, 1, 1, 2);
    assertThat(page.content())
        .singleElement()
        .extracting(NoteInfoDto::id, NoteInfoDto::title)
        .containsExactly(firstNote.getId(), firstNote.getTitle());
  }

  /**
   * Returns an owned note through the protected endpoint.
   */
  @Test
  void shouldReturnOwnedNoteAgainstPostgres() {
    var owner = findUser(OWNER_EMAIL);
    var accessToken = accessTokenGenerator.generate(owner).rawValue();
    var note = saveNote(owner, "Owned note", "Owned content");

    var response = given()
        .port(port)
        .auth()
        .oauth2(accessToken)
        .when()
        .get(NOTES_ENDPOINT + "/" + note.getId())
        .then()
        .statusCode(HttpStatus.OK.value())
        .extract()
        .as(NoteInfoDto.class);

    assertThat(response)
        .extracting(NoteInfoDto::id, NoteInfoDto::title, NoteInfoDto::content)
        .containsExactly(note.getId(), note.getTitle(), note.getContent());
  }

  /**
   * Hides another user's note behind the same not-found response as an unknown
   * note.
   */
  @Test
  void shouldRejectAccessToAnotherUsersNoteAgainstPostgres() {
    var owner = findUser(OWNER_EMAIL);
    var otherOwner = findUser(OTHER_OWNER_EMAIL);
    var accessToken = accessTokenGenerator.generate(owner).rawValue();
    var note = saveNote(otherOwner, "Private note", "Private content");

    var response = given()
        .port(port)
        .auth()
        .oauth2(accessToken)
        .when()
        .get(NOTES_ENDPOINT + "/" + note.getId())
        .then()
        .statusCode(HttpStatus.NOT_FOUND.value())
        .extract()
        .as(ApiErrorResponse.class);

    assertThat(response.code()).isEqualTo("NOTE_NOT_FOUND");
  }

  /**
   * Updates an owned note and persists the new title and Markdown content.
   */
  @Test
  void shouldUpdateOwnedNoteAgainstPostgres() {
    var owner = findUser(OWNER_EMAIL);
    var accessToken = accessTokenGenerator.generate(owner).rawValue();
    var note = saveNote(owner, "Old title", "Old content");
    var request = NoteRequest.builder()
        .title("Updated title")
        .content("## Updated content")
        .build();
    var currentEtag = given()
        .port(port)
        .auth()
        .oauth2(accessToken)
        .when()
        .get(NOTES_ENDPOINT + "/" + note.getId())
        .then()
        .statusCode(HttpStatus.OK.value())
        .extract()
        .header(HttpHeaders.ETAG);

    var response = givenWithCsrf()
        .port(port)
        .auth()
        .oauth2(accessToken)
        .contentType(ContentType.JSON)
        .header(HttpHeaders.IF_MATCH, currentEtag)
        .body(request)
        .when()
        .put(NOTES_ENDPOINT + "/" + note.getId())
        .then()
        .statusCode(HttpStatus.OK.value())
        .extract()
        .as(NoteInfoDto.class);

    assertThat(response.id()).isEqualTo(note.getId());
    assertThat(response)
        .extracting(NoteInfoDto::title, NoteInfoDto::content)
        .containsExactly(request.title(), request.content());

    var updatedNote = noteRepository.findById(note.getId()).orElseThrow();
    assertThat(updatedNote)
        .extracting(NoteEntity::getTitle, NoteEntity::getContent)
        .containsExactly(request.title(), request.content());
  }

  /**
   * Deletes an owned note and returns an empty successful response.
   */
  @Test
  void shouldDeleteOwnedNoteAgainstPostgres() {
    var owner = findUser(OWNER_EMAIL);
    var accessToken = accessTokenGenerator.generate(owner).rawValue();
    var note = saveNote(owner, "Delete me", "Delete content");

    var response = givenWithCsrf()
        .port(port)
        .auth()
        .oauth2(accessToken)
        .when()
        .delete(NOTES_ENDPOINT + "/" + note.getId())
        .then()
        .statusCode(HttpStatus.NO_CONTENT.value())
        .extract()
        .response();

    assertThat(response.asByteArray()).isEmpty();
    assertThat(noteRepository.findById(note.getId())).isEmpty();
  }

  /**
   * Rejects an invalid note request before the service is invoked.
   */
  @Test
  void shouldRejectInvalidNoteRequestAgainstPostgres() {
    var owner = findUser(OWNER_EMAIL);
    var accessToken = accessTokenGenerator.generate(owner).rawValue();
    var request = NoteRequest.builder()
        .title(" ")
        .content("Valid content")
        .build();

    var response = givenWithCsrf()
        .port(port)
        .auth()
        .oauth2(accessToken)
        .contentType(ContentType.JSON)
        .body(request)
        .when()
        .post(NOTES_ENDPOINT)
        .then()
        .statusCode(HttpStatus.BAD_REQUEST.value())
        .extract()
        .as(ApiErrorResponse.class);

    assertThat(response.code()).isEqualTo("VALIDATION_FAILED");
    assertThat(response.violations())
        .singleElement()
        .extracting(ValidationViolation::field, ValidationViolation::code)
        .containsExactly("title", "REQUIRED");
  }

  /**
   * Rejects note access when no bearer access token is supplied.
   */
  @Test
  void shouldRejectRequestWithoutAccessTokenAgainstPostgres() {
    given()
        .port(port)
        .when()
        .get(NOTES_ENDPOINT)
        .then()
        .statusCode(HttpStatus.UNAUTHORIZED.value());
  }

  private UserEntity findUser(String email) {
    return userRepository.findByEmail(email).orElseThrow();
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

  private NoteEntity saveNote(UserEntity owner, String title, String content) {
    return noteRepository.saveAndFlush(NoteEntity.builder()
        .owner(owner)
        .title(title)
        .content(content)
        .build());
  }

  private record NotePageResponse(
      List<NoteInfoDto> content,
      int number,
      int size,
      int numberOfElements,
      int totalElements) {
  }
}
