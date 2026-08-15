package com.andrii.vaultnote.app.api.users;

import com.andrii.vaultnote.app.api.users.dto.UpdateUserProfileRequest;
import com.andrii.vaultnote.app.api.users.dto.UserInfoDto;
import com.andrii.vaultnote.app.api.users.dto.UserProfileDto;
import com.andrii.vaultnote.app.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
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
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserController {

  UserService userService;

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
