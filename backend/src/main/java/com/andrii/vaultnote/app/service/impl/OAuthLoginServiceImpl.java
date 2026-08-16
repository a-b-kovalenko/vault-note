package com.andrii.vaultnote.app.service.impl;

import com.andrii.vaultnote.app.exception.OAuthLoginException;
import com.andrii.vaultnote.app.service.OAuthLoginService;
import com.andrii.vaultnote.app.service.RefreshTokenService;
import com.andrii.vaultnote.users.domain.DisplayNameRules;
import com.andrii.vaultnote.users.domain.OAuthProvider;
import com.andrii.vaultnote.users.domain.UserRole;
import com.andrii.vaultnote.users.infrastructure.persistence.entity.OAuthIdentityEntity;
import com.andrii.vaultnote.users.infrastructure.persistence.entity.UserEntity;
import com.andrii.vaultnote.users.infrastructure.persistence.repository.OAuthIdentityJpaRepository;
import com.andrii.vaultnote.users.infrastructure.persistence.repository.UserJpaRepository;
import jakarta.transaction.Transactional;
import java.util.EnumSet;
import java.util.Locale;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OAuthLoginServiceImpl implements OAuthLoginService {

  private static final String SUBJECT_CLAIM = "sub";
  private static final String EMAIL_CLAIM = "email";
  private static final String EMAIL_VERIFIED_CLAIM = "email_verified";
  private static final String NAME_CLAIM = "name";

  OAuthIdentityJpaRepository oauthIdentityRepository;
  UserJpaRepository userRepository;
  RefreshTokenService refreshTokenService;

  @Override
  @Transactional
  public String login(OidcUser oidcUser) {
    var identity = readIdentity(oidcUser);
    var user = oauthIdentityRepository
      .findByProviderAndProviderSubject(OAuthProvider.GOOGLE, identity.subject())
      .map(OAuthIdentityEntity::getUser)
      .orElseGet(() -> createUserWithIdentity(identity));

    return refreshTokenService.createSession(user);
  }

  private GoogleIdentity readIdentity(OidcUser oidcUser) {
    if (oidcUser == null) {
      throw new OAuthLoginException();
    }

    var subject = oidcUser.getClaimAsString(SUBJECT_CLAIM);
    var email = oidcUser.getClaimAsString(EMAIL_CLAIM);
    var emailVerified = oidcUser.getClaims().get(EMAIL_VERIFIED_CLAIM);

    if (!StringUtils.hasText(subject)
      || !StringUtils.hasText(email)
      || !Boolean.TRUE.equals(emailVerified)) {
      throw new OAuthLoginException();
    }

    var normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
    var displayName = displayName(oidcUser.getClaimAsString(NAME_CLAIM), normalizedEmail);

    return new GoogleIdentity(subject, normalizedEmail, displayName);
  }

  private UserEntity createUserWithIdentity(GoogleIdentity identity) {
    if (userRepository.findByEmailIgnoreCase(identity.email()).isPresent()) {
      log.warn("OAuth sign-in conflicts with an existing local account");
      throw new OAuthLoginException();
    }

    var user = userRepository.save(UserEntity.builder()
      .email(identity.email())
      .displayName(identity.displayName())
      .emailVerified(true)
      .roles(EnumSet.of(UserRole.USER))
      .build());

    oauthIdentityRepository.save(OAuthIdentityEntity.builder()
      .user(user)
      .provider(OAuthProvider.GOOGLE)
      .providerSubject(identity.subject())
      .build());

    return user;
  }

  private String displayName(String providerName, String email) {
    var fallback = email.substring(0, email.indexOf('@'));
    var candidate = StringUtils.hasText(providerName) ? providerName.trim() : fallback;
    if (candidate.length() > DisplayNameRules.MAX_LENGTH) {
      candidate = fallback;
    }

    try {
      DisplayNameRules.validate(candidate);
      return candidate;
    } catch (IllegalArgumentException exception) {
      throw new OAuthLoginException();
    }
  }

  private record GoogleIdentity(
    String subject,
    String email,
    String displayName) {
  }
}
