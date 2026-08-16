package com.andrii.vaultnote.app.service.impl;

import com.andrii.vaultnote.app.exception.OAuthLoginException;
import com.andrii.vaultnote.users.domain.OAuthProvider;
import com.andrii.vaultnote.users.domain.UserRole;
import com.andrii.vaultnote.users.infrastructure.persistence.entity.OAuthIdentityEntity;
import com.andrii.vaultnote.users.infrastructure.persistence.entity.UserEntity;
import com.andrii.vaultnote.users.infrastructure.persistence.repository.OAuthIdentityJpaRepository;
import com.andrii.vaultnote.users.infrastructure.persistence.repository.UserJpaRepository;
import jakarta.transaction.Transactional;
import java.util.EnumSet;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OAuthUserProvisioningService {

  OAuthIdentityJpaRepository oauthIdentityRepository;
  UserJpaRepository userRepository;

  @Transactional
  public UserResolution resolve(String subject, String email, String displayName) {
    var existingIdentity = oauthIdentityRepository
      .findByProviderAndProviderSubject(OAuthProvider.GOOGLE, subject)
      .map(OAuthIdentityEntity::getUser);
    if (existingIdentity.isPresent()) {
      return new UserResolution(existingIdentity.get(), false);
    }

    if (userRepository.findByEmailIgnoreCase(email).isPresent()) {
      log.warn("OAuth sign-in conflicts with an existing local account");
      throw new OAuthLoginException();
    }

    var user = userRepository.save(UserEntity.builder()
      .email(email)
      .displayName(displayName)
      .emailVerified(true)
      .roles(EnumSet.of(UserRole.USER))
      .build());

    oauthIdentityRepository.save(OAuthIdentityEntity.builder()
      .user(user)
      .provider(OAuthProvider.GOOGLE)
      .providerSubject(subject)
      .build());

    return new UserResolution(user, true);
  }

  public record UserResolution(UserEntity user, boolean created) {
  }
}
