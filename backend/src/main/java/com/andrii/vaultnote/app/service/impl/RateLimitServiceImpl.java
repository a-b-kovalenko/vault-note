package com.andrii.vaultnote.app.service.impl;

import com.andrii.vaultnote.app.config.RateLimitProperties;
import com.andrii.vaultnote.app.exception.RateLimitExceededException;
import com.andrii.vaultnote.app.security.ratelimit.RateLimitRule;
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
  public void checkLogin(String clientIp, String email) {
    if (!properties.enabled()) {
      return;
    }

    var loginProperties = properties.login();
    var rules = List.of(
      new RateLimitRule(
        "login:ip:" + normalizeClientIp(clientIp),
        loginProperties.ipLimit(),
        loginProperties.window()),
      new RateLimitRule(
        "login:email:" + normalizeEmail(email),
        loginProperties.emailLimit(),
        loginProperties.window()));
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
