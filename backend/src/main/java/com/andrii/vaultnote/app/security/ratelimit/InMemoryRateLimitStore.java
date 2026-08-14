package com.andrii.vaultnote.app.security.ratelimit;

import com.andrii.vaultnote.app.config.RateLimitProperties;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class InMemoryRateLimitStore implements RateLimitStore {

  RateLimitProperties properties;
  Map<String, WindowCounter> counters = new HashMap<>();

  @Override
  public synchronized RateLimitDecision consume(List<RateLimitRule> rules, Instant now) {
    Objects.requireNonNull(rules, "Rate-limit rules must not be null.");
    Objects.requireNonNull(now, "Current time must not be null.");
    if (rules.isEmpty()) {
      return RateLimitDecision.allow();
    }

    removeExpired(now);

    var retryAfter = retryAfterIfRejected(rules, now);
    if (!retryAfter.isZero()) {
      return RateLimitDecision.rejected(retryAfter);
    }

    var newKeys = rules.stream()
      .map(RateLimitRule::key)
      .filter(key -> !counters.containsKey(key))
      .distinct()
      .toList();
    ensureCapacity(newKeys.size());

    for (var rule : rules) {
      var counter = counters.get(rule.key());
      if (counter == null) {
        counters.put(rule.key(), new WindowCounter(1, now.plus(rule.window())));
      } else {
        counters.put(rule.key(), new WindowCounter(counter.count() + 1, counter.expiresAt()));
      }
    }

    return RateLimitDecision.allow();
  }

  synchronized int size() {
    return counters.size();
  }

  private Duration retryAfterIfRejected(List<RateLimitRule> rules, Instant now) {
    var longestRetryAfter = Duration.ZERO;

    for (var rule : rules) {
      var counter = counters.get(rule.key());
      if (counter == null || counter.count() < rule.limit()) {
        continue;
      }

      var retryAfter = Duration.between(now, counter.expiresAt());
      if (retryAfter.compareTo(longestRetryAfter) > 0) {
        longestRetryAfter = retryAfter;
      }
    }

    return longestRetryAfter;
  }

  private void ensureCapacity(int newEntries) {
    var maxEntries = properties.maxEntries();
    if (newEntries > maxEntries) {
      throw new IllegalStateException("Rate-limit request contains more keys than the store capacity.");
    }

    while (counters.size() + newEntries > maxEntries) {
      var keyToEvict = counters.entrySet().stream()
        .min(Map.Entry.comparingByValue((left, right) -> left.expiresAt().compareTo(right.expiresAt())))
        .map(Map.Entry::getKey)
        .orElseThrow(() -> new IllegalStateException("Rate-limit store capacity cannot be satisfied."));
      counters.remove(keyToEvict);
    }
  }

  private void removeExpired(Instant now) {
    counters.entrySet().removeIf(entry -> !entry.getValue().expiresAt().isAfter(now));
  }

  private record WindowCounter(int count, Instant expiresAt) {
  }
}
