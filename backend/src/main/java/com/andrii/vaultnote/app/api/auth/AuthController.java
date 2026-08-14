package com.andrii.vaultnote.app.api.auth;

import com.andrii.vaultnote.app.api.auth.dto.LoginRequest;
import com.andrii.vaultnote.app.api.auth.dto.LoginResponse;
import com.andrii.vaultnote.app.api.auth.dto.CurrentUserResponse;
import com.andrii.vaultnote.app.api.auth.dto.PasswordResetConfirmRequest;
import com.andrii.vaultnote.app.api.auth.dto.PasswordResetRequest;
import com.andrii.vaultnote.app.api.auth.dto.RegisterUserRequest;
import com.andrii.vaultnote.app.api.auth.dto.RegisterUserResponse;
import com.andrii.vaultnote.app.api.error.ApiErrorResponse;
import com.andrii.vaultnote.app.exception.RefreshTokenAuthenticationFailedException;
import com.andrii.vaultnote.app.service.EmailVerificationService;
import com.andrii.vaultnote.app.service.LoginResult;
import com.andrii.vaultnote.app.service.LoginService;
import com.andrii.vaultnote.app.service.PasswordResetService;
import com.andrii.vaultnote.app.service.RefreshTokenService;
import com.andrii.vaultnote.app.service.RegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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

  EmailVerificationService emailVerificationService;
  LoginService loginService;
  PasswordResetService passwordResetService;
  RegistrationService registrationService;
  RefreshTokenService refreshTokenService;

  RefreshTokenCookieFactory refreshTokenCookieFactory;
  RefreshTokenCookieExtractor refreshTokenCookieExtractor;

  @Operation
  @ApiResponse(
    responseCode = "201",
    description = "Register user",
    content = {
      @Content(
        mediaType = MediaType.APPLICATION_JSON_VALUE,
        schema = @Schema(implementation = RegisterUserResponse.class))})
  @ApiResponse(responseCode = "400", description = "Invalid request", content = {@Content})
  @ApiResponse(responseCode = "409", description = "Email already registered", content = {@Content})
  @ApiResponse(
    responseCode = "429",
    description = "Too many registration requests",
    content = {
      @Content(
        mediaType = MediaType.APPLICATION_JSON_VALUE,
        schema = @Schema(implementation = ApiErrorResponse.class))})
  @ApiResponse(responseCode = "500", description = "Server error", content = {@Content})

  @PostMapping("/registrations")
  @ResponseStatus(HttpStatus.CREATED)
  public RegisterUserResponse registerUser(
    HttpServletRequest servletRequest,
    @RequestBody @Valid RegisterUserRequest request) {
    return registrationService.registerUser(request, servletRequest.getRemoteAddr());
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

  @Operation(summary = "Request a password reset")
  @ApiResponse(responseCode = "202", description = "Password reset request accepted")
  @ApiResponse(
    responseCode = "400",
    description = "Invalid request",
    content = {
      @Content(
        mediaType = MediaType.APPLICATION_JSON_VALUE,
        schema = @Schema(implementation = ApiErrorResponse.class))})
  @ApiResponse(
    responseCode = "429",
    description = "Too many password reset requests",
    content = {
      @Content(
        mediaType = MediaType.APPLICATION_JSON_VALUE,
        schema = @Schema(implementation = ApiErrorResponse.class))})
  @ApiResponse(responseCode = "500", description = "Server error", content = {@Content})
  @PostMapping("/password-reset/request")
  @ResponseStatus(HttpStatus.ACCEPTED)
  public void requestPasswordReset(
    HttpServletRequest servletRequest,
    @RequestBody @Valid PasswordResetRequest request) {
    passwordResetService.requestPasswordReset(request, servletRequest.getRemoteAddr());
  }

  @Operation(summary = "Confirm a password reset")
  @ApiResponse(responseCode = "204", description = "Password reset completed")
  @ApiResponse(
    responseCode = "400",
    description = "Invalid request or reset token",
    content = {
      @Content(
        mediaType = MediaType.APPLICATION_JSON_VALUE,
        schema = @Schema(implementation = ApiErrorResponse.class))})
  @ApiResponse(responseCode = "500", description = "Server error", content = {@Content})
  @PostMapping("/password-reset/confirm")
  public ResponseEntity<Void> confirmPasswordReset(
    @RequestBody @Valid PasswordResetConfirmRequest request) {
    passwordResetService.confirmPasswordReset(request);

    return ResponseEntity
      .noContent()
      .header(HttpHeaders.SET_COOKIE, refreshTokenCookieFactory.clear().toString())
      .build();
  }

  @Operation
  @ApiResponse(
    responseCode = "200",
    description = "Login user",
    content = {
      @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = LoginResponse.class))})
  @ApiResponse(
    responseCode = "400",
    description = "Invalid request",
    content = {
      @Content(
        mediaType = MediaType.APPLICATION_JSON_VALUE,
        schema = @Schema(implementation = ApiErrorResponse.class))})
  @ApiResponse(
    responseCode = "401",
    description = "Unauthorized",
    content = {
      @Content(
        mediaType = MediaType.APPLICATION_JSON_VALUE,
        schema = @Schema(implementation = ApiErrorResponse.class))})
  @ApiResponse(
    responseCode = "429",
    description = "Too many login requests",
    content = {
      @Content(
        mediaType = MediaType.APPLICATION_JSON_VALUE,
        schema = @Schema(implementation = ApiErrorResponse.class))})
  @ApiResponse(responseCode = "500", description = "Server error", content = {@Content})

  @PostMapping("/login")
  public ResponseEntity<LoginResponse> login(
    HttpServletRequest servletRequest,
    @RequestBody @Valid LoginRequest request) {
    return authenticationResponse(loginService.login(request, servletRequest.getRemoteAddr()));
  }

  @Operation(summary = "Refresh access token")
  @ApiResponse(
    responseCode = "200",
    description = "Refresh access token",
    content = {
      @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = LoginResponse.class))})
  @ApiResponse(
    responseCode = "401",
    description = "Invalid or expired refresh token",
    content = {
      @Content(
        mediaType = MediaType.APPLICATION_JSON_VALUE,
        schema = @Schema(implementation = ApiErrorResponse.class))})
  @PostMapping("/refresh")
  public ResponseEntity<LoginResponse> refresh(HttpServletRequest request) {
    var rawRefreshToken = refreshTokenCookieExtractor.extract(request)
      .orElseThrow(RefreshTokenAuthenticationFailedException::new);

    return authenticationResponse(refreshTokenService.refresh(rawRefreshToken));
  }

  @Operation(summary = "Logout")
  @ApiResponse(responseCode = "204", description = "Logout user")
  @PostMapping("/logout")
  public ResponseEntity<Void> logout(HttpServletRequest request) {
    var rawRefreshToken = refreshTokenCookieExtractor.extract(request).orElse(null);
    refreshTokenService.logout(rawRefreshToken);

    return ResponseEntity
      .noContent()
      .header(HttpHeaders.SET_COOKIE, refreshTokenCookieFactory.clear().toString())
      .build();
  }

  @Operation
  @ApiResponse(
    responseCode = "200",
    description = "Get authenticated user",
    content = {
      @Content(
        mediaType = MediaType.APPLICATION_JSON_VALUE,
        schema = @Schema(implementation = CurrentUserResponse.class))})
  @ApiResponse(responseCode = "401", description = "Unauthorized", content = {@Content})
  @SecurityRequirement(name = "bearerAuth")
  @GetMapping("/me")
  public CurrentUserResponse currentUser(@AuthenticationPrincipal Jwt jwt) {
    return CurrentUserResponse.builder()
      .userId(Long.parseLong(Objects.requireNonNull(jwt.getSubject())))
      .roles(jwt.getClaimAsStringList("roles"))
      .build();
  }

  private ResponseEntity<LoginResponse> authenticationResponse(LoginResult loginResult) {
    return ResponseEntity
      .status(HttpStatus.OK)
      .header(HttpHeaders.SET_COOKIE,
        refreshTokenCookieFactory.create(loginResult.rawRefreshToken()).toString())
      .body(loginResult.response());
  }

}
