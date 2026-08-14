package com.andrii.vaultnote.app.api.auth;

import com.andrii.vaultnote.app.config.RefreshTokenProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RefreshTokenCookieExtractor {

  RefreshTokenProperties properties;

  public Optional<String> extract(HttpServletRequest request) {
    var cookies = request.getCookies();
    if (cookies == null) {
      return Optional.empty();
    }

    return Arrays.stream(cookies)
      .filter(cookie -> properties.cookieName().equals(cookie.getName()))
      .map(Cookie::getValue)
      .findFirst();
  }

}
