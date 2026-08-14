package com.andrii.vaultnote.app.service.impl;

import com.andrii.vaultnote.app.config.RateLimitProperties;
import com.andrii.vaultnote.app.exception.RateLimitExceededException;
import com.andrii.vaultnote.app.security.ratelimit.RateLimitRule;
import com.andrii.vaultnote.app.security.ratelimit.RateLimitStore;
import com.andrii.vaultnote.app.service.RateLimitService;
import java.time.Clock;
import java.time.Duration;
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
    check(
      "login",
      loginProperties.ipLimit(),
      loginProperties.emailLimit(),
      loginProperties.window(),
      clientIp,
      email);
  }

  @Override
  public void checkRegistration(String clientIp, String email) {
    if (!properties.enabled()) {
      return;
    }

    var registrationProperties = properties.registration();
    check(
      "registration",
      registrationProperties.ipLimit(),
      registrationProperties.emailLimit(),
      registrationProperties.window(),
      clientIp,
      email);
  }

  private void check(
    String scope,
    int ipLimit,
    int emailLimit,
    Duration window,
    String clientIp,
    String email) {
    var rules = List.of(
      new RateLimitRule(
        scope + ":ip:" + normalizeClientIp(clientIp),
        ipLimit,
        window),
      new RateLimitRule(
        scope + ":email:" + normalizeEmail(email),
        emailLimit,
        window));
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
