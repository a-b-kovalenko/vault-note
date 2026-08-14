package com.andrii.vaultnote.app.security.ratelimit;

import java.time.Instant;
import java.util.List;

public interface RateLimitStore {

  RateLimitDecision consume(List<RateLimitRule> rules, Instant now);
}
