package com.andrii.vaultnote.app.security.ratelimit;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Getter
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum RateLimitScope {

  LOGIN("login"), REGISTRATION("registration"), PASSWORD_RESET("password-reset");

  String key;
}
