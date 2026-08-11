package com.andrii.vaultnote.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.andrii.vaultnote.app.api.auth.dto.TokenType;
import com.andrii.vaultnote.app.security.AccessTokenGenerator;
import com.andrii.vaultnote.users.infrastructure.persistence.entity.UserEntity;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@FieldDefaults(level = AccessLevel.PRIVATE)
class AuthenticationResultFactoryTest {

  private static final String ACCESS_TOKEN = "access-token";
  private static final String RAW_REFRESH_TOKEN = "raw-refresh-token";
  private static final long ACCESS_TOKEN_EXPIRES_IN = 900L;

  @Mock
  AccessTokenGenerator accessTokenGenerator;

  @InjectMocks
  AuthenticationResultFactory authenticationResultFactory;

  @Test
  void shouldCreateAuthenticationResult() {
    var user = UserEntity.builder()
        .id(1L)
        .build();
    var generatedAccessToken = new AccessTokenGenerator.GeneratedToken(
        ACCESS_TOKEN,
        ACCESS_TOKEN_EXPIRES_IN);
    when(accessTokenGenerator.generate(user)).thenReturn(generatedAccessToken);

    var result = authenticationResultFactory.create(user, RAW_REFRESH_TOKEN);

    assertThat(result.rawRefreshToken()).isEqualTo(RAW_REFRESH_TOKEN);
    assertThat(result.response().accessToken()).isEqualTo(ACCESS_TOKEN);
    assertThat(result.response().tokenType()).isEqualTo(TokenType.BEARER);
    assertThat(result.response().expiresIn()).isEqualTo(ACCESS_TOKEN_EXPIRES_IN);
    verify(accessTokenGenerator).generate(user);
  }
}
