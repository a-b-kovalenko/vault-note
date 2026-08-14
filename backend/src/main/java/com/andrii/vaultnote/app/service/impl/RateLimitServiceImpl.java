package com.andrii.vaultnote.app.service.impl;

import com.andrii.vaultnote.app.config.RateLimitProperties;
import com.andrii.vaultnote.app.exception.RateLimitExceededException;
import com.andrii.vaultnote.app.security.ratelimit.RateLimitRule;
import com.andrii.vaultnote.app.security.ratelimit.RateLimitScope;
import com.andrii.vaultnote.app.security.ratelimit.RateLimitStore;
import com.andrii.vaultnote.app.service.RateLimitService;
import java.time.Clock;
import java.util.List;
import java.util.Locale;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RateLimitServiceImpl implements RateLimitService {

  RateLimitProperties properties;
  RateLimitStore store;
  Clock clock;

  @Override
  public void check(RateLimitScope scope, String clientIp, String email) {
    var limits = switch (scope) {
      case LOGIN -> properties.login();
      case REGISTRATION -> properties.registration();
      case PASSWORD_RESET -> properties.passwordReset();
    };
    enforce(scope.getKey(), limits, clientIp, email);
  }

  private void enforce(
    String scope,
    RateLimitProperties.Limits limits,
    String clientIp,
    String email) {
    if (!properties.enabled()) {
      return;
    }

    var rules = List.of(
      new RateLimitRule(
        scope + ":ip:" + normalizeClientIp(clientIp),
        limits.ipLimit(),
        limits.window()),
      new RateLimitRule(
        scope + ":email:" + normalizeEmail(email),
        limits.emailLimit(),
        limits.window()));
    var decision = store.consume(rules, clock.instant());

    if (!decision.allowed()) {
      throw new RateLimitExceededException(decision.retryAfter());
    }
  }

  private static String normalizeClientIp(String clientIp) {
    return clientIp == null || clientIp.isBlank() ? "unknown" : clientIp.trim();
  }

  private static String normalizeEmail(String email) {
    return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
  }
}
