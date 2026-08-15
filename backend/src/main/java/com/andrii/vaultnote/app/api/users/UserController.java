package com.andrii.vaultnote.app.api.users;

import com.andrii.vaultnote.app.api.users.dto.UpdateUserProfileRequest;
import com.andrii.vaultnote.app.api.users.dto.UserAvatarDto;
import com.andrii.vaultnote.app.api.users.dto.UserInfoDto;
import com.andrii.vaultnote.app.api.users.dto.UserProfileDto;
import com.andrii.vaultnote.app.api.error.ApiErrorResponse;
import com.andrii.vaultnote.app.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserController {

  private static final String X_CONTENT_TYPE_OPTIONS = "X-Content-Type-Options";

  UserService userService;
  MultipartAvatarUploadReader avatarUploadReader;

  @Operation(summary = "Get current user profile")
  @ApiResponse(responseCode = "200", description = "Get current user profile")
  @ApiResponse(responseCode = "401", description = "Unauthorized", content = {@Content})
  @ApiResponse(responseCode = "404", description = "User not found", content = {@Content})
  @SecurityRequirement(name = "bearerAuth")
  @GetMapping("/me")
  public UserProfileDto getCurrentProfile() {
    return userService.getCurrentProfile();
  }

  @Operation(summary = "Update current user profile")
  @ApiResponse(responseCode = "200", description = "Update current user profile")
  @ApiResponse(responseCode = "400", description = "Invalid request", content = {@Content})
  @ApiResponse(responseCode = "401", description = "Unauthorized", content = {@Content})
  @ApiResponse(responseCode = "404", description = "User not found", content = {@Content})
  @SecurityRequirement(name = "bearerAuth")
  @PatchMapping("/me")
  public UserProfileDto updateCurrentProfile(
    @RequestBody @Valid UpdateUserProfileRequest request) {
    return userService.updateCurrentProfile(request.displayName());
  }

  @Operation(summary = "Upload or replace current user avatar")
  @ApiResponse(
    responseCode = "200",
    description = "Avatar uploaded",
    content = {
      @Content(
        mediaType = MediaType.APPLICATION_JSON_VALUE,
        schema = @Schema(implementation = UserAvatarDto.class))})
  @ApiResponse(
    responseCode = "400",
    description = "Invalid avatar",
    content = {
      @Content(
        mediaType = MediaType.APPLICATION_JSON_VALUE,
        schema = @Schema(implementation = ApiErrorResponse.class))})
  @ApiResponse(responseCode = "401", description = "Unauthorized", content = {@Content})
  @ApiResponse(responseCode = "413", description = "Avatar is too large", content = {@Content})
  @SecurityRequirement(name = "bearerAuth")
  @PutMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public UserAvatarDto uploadCurrentAvatar(@RequestPart("file") MultipartFile file) {
    return userService.uploadCurrentAvatar(avatarUploadReader.read(file));
  }

  @Operation(summary = "Get current user avatar")
  @ApiResponse(
    responseCode = "200",
    description = "Current user avatar",
    content = {@Content(mediaType = MediaType.IMAGE_JPEG_VALUE)})
  @ApiResponse(responseCode = "401", description = "Unauthorized", content = {@Content})
  @ApiResponse(responseCode = "404", description = "Avatar not found", content = {@Content})
  @SecurityRequirement(name = "bearerAuth")
  @GetMapping(value = "/me/avatar", produces = MediaType.IMAGE_JPEG_VALUE)
  public ResponseEntity<byte[]> getCurrentAvatar() {
    var content = userService.getCurrentAvatar();
    return ResponseEntity.ok()
      .contentType(MediaType.IMAGE_JPEG)
      .contentLength(content.length)
      .header(X_CONTENT_TYPE_OPTIONS, "nosniff")
      .body(content);
  }

  @Operation(summary = "Delete current user avatar")
  @ApiResponse(responseCode = "204", description = "Avatar deleted")
  @ApiResponse(responseCode = "401", description = "Unauthorized", content = {@Content})
  @SecurityRequirement(name = "bearerAuth")
  @DeleteMapping("/me/avatar")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteCurrentAvatar() {
    userService.deleteCurrentAvatar();
  }

  @Operation(summary = "Get users")
  @ApiResponse(responseCode = "200", description = "Get users")
  @ApiResponse(responseCode = "401", description = "Unauthorized", content = {@Content})
  @ApiResponse(responseCode = "403", description = "Forbidden", content = {@Content})
  @SecurityRequirement(name = "bearerAuth")
  @GetMapping
  public Page<UserInfoDto> getUsers(
    @PageableDefault(size = 20) @SortDefault(
      sort = "displayName",
      direction = Sort.Direction.ASC) @ParameterObject Pageable pageable) {
    return userService.getUsers(pageable);
  }
}
