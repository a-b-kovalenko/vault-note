package com.andrii.vaultnote.app.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.andrii.vaultnote.app.exception.OAuthLoginException;
import com.andrii.vaultnote.users.domain.OAuthProvider;
import com.andrii.vaultnote.users.domain.UserRole;
import com.andrii.vaultnote.users.infrastructure.persistence.entity.OAuthIdentityEntity;
import com.andrii.vaultnote.users.infrastructure.persistence.entity.UserEntity;
import com.andrii.vaultnote.users.infrastructure.persistence.repository.OAuthIdentityJpaRepository;
import com.andrii.vaultnote.users.infrastructure.persistence.repository.UserJpaRepository;
import java.util.EnumSet;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@FieldDefaults(level = AccessLevel.PRIVATE)
class OAuthUserProvisioningServiceTest {

  private static final String SUBJECT = "google-subject-123";
  private static final String EMAIL = "user@example.com";
  private static final String DISPLAY_NAME = "Andrii User";

  @Mock
  OAuthIdentityJpaRepository oauthIdentityRepository;
  @Mock
  UserJpaRepository userRepository;

  @InjectMocks
  OAuthUserProvisioningService provisioningService;

  @Test
  void shouldResolveExistingOAuthIdentity() {
    var user = user(1L);
    var identity = OAuthIdentityEntity.builder()
      .user(user)
      .provider(OAuthProvider.GOOGLE)
      .providerSubject(SUBJECT)
      .build();

    when(oauthIdentityRepository.findByProviderAndProviderSubject(
      OAuthProvider.GOOGLE,
      SUBJECT)).thenReturn(Optional.of(identity));

    var result = provisioningService.resolve(SUBJECT, EMAIL, DISPLAY_NAME);

    assertThat(result.user()).isSameAs(user);
    assertThat(result.created()).isFalse();
    verifyNoInteractions(userRepository);
  }

  @Test
  void shouldCreateUserAndOAuthIdentityForNewGoogleUser() {
    var savedUser = user(2L);

    when(oauthIdentityRepository.findByProviderAndProviderSubject(
      OAuthProvider.GOOGLE,
      SUBJECT)).thenReturn(Optional.empty());
    when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.empty());
    when(userRepository.save(any(UserEntity.class))).thenReturn(savedUser);

    var result = provisioningService.resolve(SUBJECT, EMAIL, DISPLAY_NAME);

    assertThat(result.user()).isSameAs(savedUser);
    assertThat(result.created()).isTrue();

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
  }

  @Test
  void shouldRejectGoogleIdentityWhenEmailBelongsToLocalUser() {
    var localUser = user(3L);

    when(oauthIdentityRepository.findByProviderAndProviderSubject(
      OAuthProvider.GOOGLE,
      SUBJECT)).thenReturn(Optional.empty());
    when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(localUser));

    assertThatExceptionOfType(OAuthLoginException.class)
      .isThrownBy(() -> provisioningService.resolve(SUBJECT, EMAIL, DISPLAY_NAME))
      .withMessage("Unable to complete OAuth sign-in.");

    verify(userRepository).findByEmailIgnoreCase(EMAIL);
    verify(userRepository, never()).save(any(UserEntity.class));
    verify(oauthIdentityRepository).findByProviderAndProviderSubject(
      OAuthProvider.GOOGLE,
      SUBJECT);
  }

  private static UserEntity user(Long id) {
    return UserEntity.builder()
      .id(id)
      .email(EMAIL)
      .roles(EnumSet.of(UserRole.USER))
      .build();
  }
}
