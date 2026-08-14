package com.andrii.vaultnote.app.service;

import com.andrii.vaultnote.app.security.ratelimit.RateLimitScope;

public interface RateLimitService {

  void check(RateLimitScope scope, String clientIp, String email);
}
