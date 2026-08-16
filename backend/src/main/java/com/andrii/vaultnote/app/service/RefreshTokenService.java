package com.andrii.vaultnote.app.service;

import com.andrii.vaultnote.users.infrastructure.persistence.entity.UserEntity;

public interface RefreshTokenService {

  String createSession(UserEntity user);

  LoginResult refresh(String rawRefreshToken);

  void logout(String rawRefreshToken);

}
