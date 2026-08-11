package com.andrii.vaultnote.app.api.auth;

import com.andrii.vaultnote.app.config.RefreshTokenProperties;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RefreshTokenCookieFactory {

  RefreshTokenProperties properties;

  public ResponseCookie create(String rawRefreshToken) {
    return ResponseCookie.from(properties.cookieName(), rawRefreshToken)
        .httpOnly(true)
        .secure(properties.secure())
        .path(properties.cookiePath())
        .sameSite(properties.sameSite())
        .maxAge(properties.ttl())
        .build();
  }

  public ResponseCookie clear() {
    return ResponseCookie.from(properties.cookieName(), "")
        .httpOnly(true)
        .secure(properties.secure())
        .path(properties.cookiePath())
        .sameSite(properties.sameSite())
        .maxAge(Duration.ZERO)
        .build();
  }
}
