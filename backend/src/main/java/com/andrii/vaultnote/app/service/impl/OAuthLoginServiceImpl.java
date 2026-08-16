package com.andrii.vaultnote.app.service.impl;

import com.andrii.vaultnote.app.exception.OAuthLoginException;
import com.andrii.vaultnote.app.service.OAuthLoginService;
import com.andrii.vaultnote.app.service.RefreshTokenService;
import com.andrii.vaultnote.users.domain.DisplayNameRules;
import java.util.Locale;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OAuthLoginServiceImpl implements OAuthLoginService {

  private static final String SUBJECT_CLAIM = "sub";
  private static final String EMAIL_CLAIM = "email";
  private static final String EMAIL_VERIFIED_CLAIM = "email_verified";
  private static final String NAME_CLAIM = "name";
  private static final String PICTURE_CLAIM = "picture";

  OAuthUserProvisioningService userProvisioningService;
  GoogleAvatarImporter googleAvatarImporter;
  RefreshTokenService refreshTokenService;

  @Override
  public String login(OidcUser oidcUser) {
    var identity = readIdentity(oidcUser);
    var resolution = userProvisioningService.resolve(
      identity.subject(),
      identity.email(),
      identity.displayName());
    googleAvatarImporter.importIfAvailable(resolution.user().getId(), identity.pictureUrl());

    return refreshTokenService.createSession(resolution.user());
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
    var pictureUrl = oidcUser.getClaimAsString(PICTURE_CLAIM);

    return new GoogleIdentity(subject.trim(), normalizedEmail, displayName, pictureUrl);
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
    String displayName,
    String pictureUrl) {
  }
}
