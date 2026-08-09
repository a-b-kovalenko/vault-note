package com.andrii.vaultnote.app.api.auth;

import com.andrii.vaultnote.app.api.auth.dto.RegisterUserRequest;
import com.andrii.vaultnote.app.api.auth.dto.RegisterUserResponse;
import com.andrii.vaultnote.app.service.EmailVerificationService;
import com.andrii.vaultnote.app.service.RegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RegistrationController {

  RegistrationService registrationService;
  EmailVerificationService verificationService;

  @Operation
  @ApiResponse(responseCode = "201", description = "Register user", content = {
      @Content(mediaType = "application/json", schema = @Schema(implementation = RegisterUserResponse.class))})
  @ApiResponse(responseCode = "400", description = "Invalid request", content = {@Content})
  @ApiResponse(responseCode = "401", description = "Unauthorized", content = {@Content})
  @ApiResponse(responseCode = "404", description = "Not found", content = {@Content})
  @ApiResponse(responseCode = "409", description = "Email already registered", content = {@Content})
  @ApiResponse(responseCode = "500", description = "Server error", content = {@Content})

  @PostMapping("/registrations")
  @ResponseStatus(HttpStatus.CREATED)
  public RegisterUserResponse registerUser(@RequestBody @Valid RegisterUserRequest request) {
    return registrationService.registerUser(request);
  }

  @Operation
  @ApiResponse(responseCode = "204", description = "Verify user's email", content = {
      @Content(mediaType = "application/json", schema = @Schema(implementation = RegisterUserResponse.class))})
  @ApiResponse(responseCode = "400", description = "Invalid request", content = {@Content})
  @ApiResponse(responseCode = "404", description = "Not found", content = {@Content})
  @ApiResponse(responseCode = "500", description = "Server error", content = {@Content})

  @PostMapping("/email-verification")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void verifyEmail(@RequestParam(name = "token") String token) {
    verificationService.verifyEmail(token);
  }
}
