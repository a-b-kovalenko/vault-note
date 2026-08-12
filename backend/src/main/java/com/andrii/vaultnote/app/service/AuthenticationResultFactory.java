package com.andrii.vaultnote.app.service;

import com.andrii.vaultnote.app.api.auth.dto.LoginResponse;
import com.andrii.vaultnote.app.api.auth.dto.TokenType;
import com.andrii.vaultnote.app.security.AccessTokenGenerator;
import com.andrii.vaultnote.users.infrastructure.persistence.entity.UserEntity;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationResultFactory {

  AccessTokenGenerator accessTokenGenerator;

  public LoginResult create(UserEntity user, String rawRefreshToken) {
    var accessToken = accessTokenGenerator.generate(user);

    var loginResponse = LoginResponse.builder()
      .accessToken(accessToken.rawValue())
      .tokenType(TokenType.BEARER)
      .expiresIn(accessToken.expiresIn())
      .build();

    return new LoginResult(loginResponse, rawRefreshToken);
  }
}
