package com.andrii.vaultnote.app.api.security;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CsrfController {

  @Operation(
    summary = "Get CSRF token",
    description = "Returns a short-lived CSRF token for browser clients. The token is not persisted in a cookie or server-side session.")
  @ApiResponse(responseCode = "200", description = "CSRF token issued")
  @GetMapping("/csrf")
  public CsrfToken csrf(CsrfToken csrfToken) {
    return csrfToken;
  }
}
