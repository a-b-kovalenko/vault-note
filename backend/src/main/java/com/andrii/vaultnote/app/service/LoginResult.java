package com.andrii.vaultnote.app.service;

import com.andrii.vaultnote.app.api.auth.dto.LoginResponse;

public record LoginResult(
  LoginResponse response,
  String rawRefreshToken) {
}
