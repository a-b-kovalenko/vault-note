package com.andrii.vaultnote.app.api.auth;

import com.andrii.vaultnote.app.api.auth.dto.LoginRequest;
import com.andrii.vaultnote.app.api.auth.dto.LoginResponse;
import com.andrii.vaultnote.app.api.auth.dto.CurrentUserResponse;
import com.andrii.vaultnote.app.api.auth.dto.RegisterUserRequest;
import com.andrii.vaultnote.app.api.auth.dto.RegisterUserResponse;
import com.andrii.vaultnote.app.service.EmailVerificationService;
import com.andrii.vaultnote.app.service.LoginService;
import com.andrii.vaultnote.app.service.RegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Objects;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthController {

  RegistrationService registrationService;
  EmailVerificationService emailVerificationService;
  LoginService loginService;
  RefreshTokenCookieFactory refreshTokenCookieFactory;

  @Operation
  @ApiResponse(responseCode = "201", description = "Register user", content = {
      @Content(mediaType = "application/json", schema = @Schema(implementation = RegisterUserResponse.class))})
  @ApiResponse(responseCode = "400", description = "Invalid request", content = {@Content})
  @ApiResponse(responseCode = "409", description = "Email already registered", content = {@Content})
  @ApiResponse(responseCode = "500", description = "Server error", content = {@Content})

  @PostMapping("/registrations")
  @ResponseStatus(HttpStatus.CREATED)
  public RegisterUserResponse registerUser(@RequestBody @Valid RegisterUserRequest request) {
    return registrationService.registerUser(request);
  }

  @Operation
  @ApiResponse(responseCode = "204", description = "Verify user's email")
  @ApiResponse(responseCode = "400", description = "Invalid request", content = {@Content})
  @ApiResponse(responseCode = "404", description = "Not found", content = {@Content})
  @ApiResponse(responseCode = "500", description = "Server error", content = {@Content})

  @PostMapping("/email-verification")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void verifyEmail(@RequestParam(name = "token") String token) {
    emailVerificationService.verifyEmail(token);
  }

  @Operation
  @ApiResponse(responseCode = "200", description = "Login user", content = {
      @Content(mediaType = "application/json", schema = @Schema(implementation = LoginResponse.class))})
  @ApiResponse(responseCode = "400", description = "Invalid request", content = {@Content})
  @ApiResponse(responseCode = "401", description = "Unauthorized", content = {@Content})
  @ApiResponse(responseCode = "500", description = "Server error", content = {@Content})

  @PostMapping("/login")
  public ResponseEntity<LoginResponse> login(@RequestBody @Valid LoginRequest request) {
    var loginResult = loginService.login(request);

    return ResponseEntity
        .status(HttpStatus.OK)
        .header(HttpHeaders.SET_COOKIE,
            refreshTokenCookieFactory.create(loginResult.rawRefreshToken()).toString())
        .body(loginResult.response());
  }

  @Operation
  @ApiResponse(responseCode = "200", description = "Get authenticated user", content = {
      @Content(mediaType = "application/json", schema = @Schema(implementation = CurrentUserResponse.class))})
  @ApiResponse(responseCode = "401", description = "Unauthorized", content = {@Content})
  @SecurityRequirement(name = "bearerAuth")
  @GetMapping("/me")
  public CurrentUserResponse currentUser(@AuthenticationPrincipal Jwt jwt) {
    return CurrentUserResponse.builder()
        .userId(Long.parseLong(Objects.requireNonNull(jwt.getSubject())))
        .roles(jwt.getClaimAsStringList("roles"))
        .build();
  }

}
