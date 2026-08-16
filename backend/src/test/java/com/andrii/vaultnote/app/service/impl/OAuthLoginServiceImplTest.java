package com.andrii.vaultnote.app.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.andrii.vaultnote.app.exception.OAuthLoginException;
import com.andrii.vaultnote.app.service.RefreshTokenService;
import com.andrii.vaultnote.users.domain.OAuthProvider;
import com.andrii.vaultnote.users.domain.UserRole;
import com.andrii.vaultnote.users.infrastructure.persistence.entity.OAuthIdentityEntity;
import com.andrii.vaultnote.users.infrastructure.persistence.entity.UserEntity;
import com.andrii.vaultnote.users.infrastructure.persistence.repository.OAuthIdentityJpaRepository;
import com.andrii.vaultnote.users.infrastructure.persistence.repository.UserJpaRepository;
import java.util.Map;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
  private static final String RAW_REFRESH_TOKEN = "raw-refresh-token";

  @Mock
  OAuthIdentityJpaRepository oauthIdentityRepository;
  @Mock
  UserJpaRepository userRepository;
  @Mock
  RefreshTokenService refreshTokenService;

  @InjectMocks
  OAuthLoginServiceImpl oauthLoginService;

  @Test
  void shouldCreateSessionForExistingOAuthIdentity() {
    var user = UserEntity.builder()
      .id(1L)
      .email(EMAIL)
      .build();
    var identity = OAuthIdentityEntity.builder()
      .user(user)
      .provider(OAuthProvider.GOOGLE)
      .providerSubject(SUBJECT)
      .build();
    var oidcUser = oidcUser(true);

    when(oauthIdentityRepository.findByProviderAndProviderSubject(
      OAuthProvider.GOOGLE,
      SUBJECT)).thenReturn(Optional.of(identity));
    when(refreshTokenService.createSession(user)).thenReturn(RAW_REFRESH_TOKEN);

    var result = oauthLoginService.login(oidcUser);

    assertThat(result).isEqualTo(RAW_REFRESH_TOKEN);
    verify(refreshTokenService).createSession(user);
    verifyNoInteractions(userRepository);
  }

  @Test
  void shouldCreateUserAndOAuthIdentityForNewGoogleUser() {
    var savedUser = UserEntity.builder()
      .id(2L)
      .email(EMAIL)
      .build();
    var oidcUser = oidcUser(true);

    when(oauthIdentityRepository.findByProviderAndProviderSubject(
      OAuthProvider.GOOGLE,
      SUBJECT)).thenReturn(Optional.empty());
    when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.empty());
    when(userRepository.save(any(UserEntity.class))).thenReturn(savedUser);
    when(refreshTokenService.createSession(savedUser)).thenReturn(RAW_REFRESH_TOKEN);

    var result = oauthLoginService.login(oidcUser);

    assertThat(result).isEqualTo(RAW_REFRESH_TOKEN);

    var userCaptor = ArgumentCaptor.forClass(UserEntity.class);
    verify(userRepository).save(userCaptor.capture());
    var createdUser = userCaptor.getValue();
    assertThat(createdUser.getEmail()).isEqualTo(EMAIL);
    assertThat(createdUser.getDisplayName()).isEqualTo(DISPLAY_NAME);
    assertThat(createdUser.getPasswordHash()).isNull();
    assertThat(createdUser.isEmailVerified()).isTrue();
    assertThat(createdUser.getRoles()).containsExactly(UserRole.USER);

    var identityCaptor = ArgumentCaptor.forClass(OAuthIdentityEntity.class);
    verify(oauthIdentityRepository).save(identityCaptor.capture());
    var createdIdentity = identityCaptor.getValue();
    assertThat(createdIdentity.getUser()).isSameAs(savedUser);
    assertThat(createdIdentity.getProvider()).isEqualTo(OAuthProvider.GOOGLE);
    assertThat(createdIdentity.getProviderSubject()).isEqualTo(SUBJECT);
    verify(refreshTokenService).createSession(savedUser);
  }

  @Test
  void shouldRejectGoogleIdentityWhenEmailBelongsToLocalUser() {
    var localUser = UserEntity.builder()
      .id(3L)
      .email(EMAIL)
      .build();
    var oidcUser = oidcUser(true);

    when(oauthIdentityRepository.findByProviderAndProviderSubject(
      OAuthProvider.GOOGLE,
      SUBJECT)).thenReturn(Optional.empty());
    when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(localUser));

    assertThatExceptionOfType(OAuthLoginException.class)
      .isThrownBy(() -> oauthLoginService.login(oidcUser))
      .withMessage("Unable to complete OAuth sign-in.");

    verifyNoInteractions(refreshTokenService);
  }

  @Test
  void shouldRejectUnverifiedGoogleEmail() {
    var oidcUser = oidcUser(false);

    assertThatExceptionOfType(OAuthLoginException.class)
      .isThrownBy(() -> oauthLoginService.login(oidcUser));

    verifyNoInteractions(oauthIdentityRepository, userRepository, refreshTokenService);
  }

  private static OidcUser oidcUser(boolean emailVerified) {
    var oidcUser = org.mockito.Mockito.mock(OidcUser.class);
    when(oidcUser.getClaimAsString("sub")).thenReturn(SUBJECT);
    when(oidcUser.getClaimAsString("email")).thenReturn(EMAIL);
    if (emailVerified) {
      when(oidcUser.getClaimAsString("name")).thenReturn(DISPLAY_NAME);
    }
    when(oidcUser.getClaims()).thenReturn(Map.of("email_verified", emailVerified));
    return oidcUser;
  }
}
