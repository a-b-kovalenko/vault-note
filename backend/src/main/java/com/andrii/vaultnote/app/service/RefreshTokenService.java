package com.andrii.vaultnote.app.service;

public interface RefreshTokenService {

  LoginResult refresh(String rawRefreshToken);

  void logout(String rawRefreshToken);

}
