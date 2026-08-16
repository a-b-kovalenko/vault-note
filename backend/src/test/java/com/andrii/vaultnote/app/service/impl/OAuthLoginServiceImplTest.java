package com.andrii.vaultnote.app.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.andrii.vaultnote.app.exception.OAuthLoginException;
import com.andrii.vaultnote.app.service.RefreshTokenService;
import com.andrii.vaultnote.users.infrastructure.persistence.entity.UserEntity;
import java.util.Map;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

@ExtendWith(MockitoExtension.class)
@FieldDefaults(level = AccessLevel.PRIVATE)
class OAuthLoginServiceImplTest {

  private static final String SUBJECT = "google-subject-123";
  private static final String EMAIL = "user@example.com";
  private static final String DISPLAY_NAME = "Andrii User";
  private static final String PICTURE_URL = "https://lh3.googleusercontent.com/avatar";
  private static final String RAW_REFRESH_TOKEN = "raw-refresh-token";

  @Mock
  OAuthUserProvisioningService userProvisioningService;
  @Mock
  GoogleAvatarImporter googleAvatarImporter;
  @Mock
  RefreshTokenService refreshTokenService;

  @InjectMocks
  OAuthLoginServiceImpl oauthLoginService;

  @Test
  void shouldImportGoogleAvatarForExistingUserWhenMissing() {
    var user = user(1L);
    var oidcUser = oidcUser(true);
    var resolution = new OAuthUserProvisioningService.UserResolution(user, false);

    when(userProvisioningService.resolve(SUBJECT, EMAIL, DISPLAY_NAME)).thenReturn(resolution);
    when(refreshTokenService.createSession(user)).thenReturn(RAW_REFRESH_TOKEN);

    var result = oauthLoginService.login(oidcUser);

    assertThat(result).isEqualTo(RAW_REFRESH_TOKEN);
    verify(googleAvatarImporter).importIfAvailable(user.getId(), PICTURE_URL);
    verify(refreshTokenService).createSession(user);
  }

  @Test
  void shouldImportGoogleAvatarForNewUser() {
    var user = user(2L);
    var oidcUser = oidcUser(true);
    var resolution = new OAuthUserProvisioningService.UserResolution(user, true);

    when(userProvisioningService.resolve(SUBJECT, EMAIL, DISPLAY_NAME)).thenReturn(resolution);
    when(refreshTokenService.createSession(user)).thenReturn(RAW_REFRESH_TOKEN);

    var result = oauthLoginService.login(oidcUser);

    assertThat(result).isEqualTo(RAW_REFRESH_TOKEN);
    verify(googleAvatarImporter).importIfAvailable(user.getId(), PICTURE_URL);
    verify(refreshTokenService).createSession(user);
  }

  @Test
  void shouldRejectUnverifiedGoogleEmail() {
    var oidcUser = oidcUser(false);

    assertThatExceptionOfType(OAuthLoginException.class)
      .isThrownBy(() -> oauthLoginService.login(oidcUser));

    verifyNoInteractions(userProvisioningService, googleAvatarImporter, refreshTokenService);
  }

  private static UserEntity user(Long id) {
    return UserEntity.builder()
      .id(id)
      .email(EMAIL)
      .build();
  }

  private static OidcUser oidcUser(boolean emailVerified) {
    var oidcUser = org.mockito.Mockito.mock(OidcUser.class);
    when(oidcUser.getClaimAsString("sub")).thenReturn(SUBJECT);
    when(oidcUser.getClaimAsString("email")).thenReturn(EMAIL);
    if (emailVerified) {
      when(oidcUser.getClaimAsString("name")).thenReturn(DISPLAY_NAME);
      when(oidcUser.getClaimAsString("picture")).thenReturn(PICTURE_URL);
    }
    when(oidcUser.getClaims()).thenReturn(Map.of("email_verified", emailVerified));
    return oidcUser;
  }
}
