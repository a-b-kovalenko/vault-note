package com.andrii.vaultnote.app.service;

public interface RateLimitService {

  void checkLogin(String clientIp, String email);

  void checkRegistration(String clientIp, String email);
}
