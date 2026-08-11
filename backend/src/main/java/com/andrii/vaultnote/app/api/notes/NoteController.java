package com.andrii.vaultnote.app.api.notes;

import com.andrii.vaultnote.app.api.notes.dto.NoteInfoDto;
import com.andrii.vaultnote.app.api.notes.dto.NoteRequest;
import com.andrii.vaultnote.app.service.NoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springdoc.core.annotations.ParameterObject;

@RestController
@RequestMapping("/api/v1/notes")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@SecurityRequirement(name = "bearerAuth")
public class NoteController {

  NoteService noteService;

  @Operation(summary = "Get current user's notes")
  @ApiResponse(responseCode = "200", description = "Get current user's notes", content = {
      @Content(mediaType = "application/json", schema = @Schema(implementation = Page.class))})
  @ApiResponse(responseCode = "401", description = "Unauthorized", content = {@Content})
  @GetMapping
  public Page<NoteInfoDto> getNotes(
      @PageableDefault(size = 20) @SortDefault(sort = "updatedAt", direction = Sort.Direction.DESC) @ParameterObject Pageable pageable) {
    return noteService.getNotes(pageable);
  }

  @Operation(summary = "Get current user's note")
  @ApiResponse(responseCode = "200", description = "Get current user's note", content = {
      @Content(mediaType = "application/json", schema = @Schema(implementation = NoteInfoDto.class))})
  @ApiResponse(responseCode = "401", description = "Unauthorized", content = {@Content})
  @ApiResponse(responseCode = "404", description = "Note not found", content = {@Content})
  @GetMapping("/{noteId}")
  public ResponseEntity<NoteInfoDto> getNote(@PathVariable Long noteId) {
    return withEtag(HttpStatus.OK, noteService.getNote(noteId));
  }

  @Operation(summary = "Create note")
  @ApiResponse(responseCode = "201", description = "Note created", content = {
      @Content(mediaType = "application/json", schema = @Schema(implementation = NoteInfoDto.class))})
  @ApiResponse(responseCode = "400", description = "Invalid request", content = {@Content})
  @ApiResponse(responseCode = "401", description = "Unauthorized", content = {@Content})
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ResponseEntity<NoteInfoDto> createNote(@RequestBody @Valid NoteRequest request) {
    return withEtag(
        HttpStatus.CREATED,
        noteService.createNote(request.title(), request.content()));
  }

  @Operation(summary = "Update note")
  @ApiResponse(responseCode = "200", description = "Note updated", content = {
      @Content(mediaType = "application/json", schema = @Schema(implementation = NoteInfoDto.class))})
  @ApiResponse(responseCode = "400", description = "Invalid request", content = {@Content})
  @ApiResponse(responseCode = "401", description = "Unauthorized", content = {@Content})
  @ApiResponse(responseCode = "404", description = "Note not found", content = {@Content})
  @PutMapping("/{noteId}")
  public ResponseEntity<NoteInfoDto> updateNote(
      @PathVariable Long noteId,
      @RequestBody @Valid NoteRequest request,
      @RequestHeader(HttpHeaders.IF_MATCH) String ifMatch) {
    var expectedVersion = NoteEtag.parse(ifMatch).version();
    return withEtag(
        HttpStatus.OK,
        noteService.updateNote(noteId, request.title(), request.content(), expectedVersion));
  }

  @Operation(summary = "Delete note")
  @ApiResponse(responseCode = "204", description = "Note deleted")
  @ApiResponse(responseCode = "401", description = "Unauthorized", content = {@Content})
  @ApiResponse(responseCode = "404", description = "Note not found", content = {@Content})
  @DeleteMapping("/{noteId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteNote(@PathVariable Long noteId) {
    noteService.deleteNote(noteId);
  }

  private static ResponseEntity<NoteInfoDto> withEtag(HttpStatus status, NoteInfoDto note) {
    return ResponseEntity.status(status)
        .eTag(new NoteEtag(note.version()).headerValue())
        .body(note);
  }
}
