package com.andrii.vaultnote.app.service;

import org.springframework.security.oauth2.core.oidc.user.OidcUser;

public interface OAuthLoginService {

  String login(OidcUser oidcUser);
}
