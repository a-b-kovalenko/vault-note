package com.andrii.vaultnote.app.security;

import com.andrii.vaultnote.app.config.JwtProperties;
import com.andrii.vaultnote.users.domain.UserRole;
import com.andrii.vaultnote.users.infrastructure.persistence.entity.UserEntity;
import java.time.Clock;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AccessTokenGenerator {

  Clock clock;
  JwtEncoder jwtEncoder;
  JwtProperties jwtProperties;

  public GeneratedToken generate(UserEntity user) {
    var issuedAt = clock.instant();
    var expiresAt = issuedAt.plus(jwtProperties.accessTokenTtl());
    var claims = JwtClaimsSet.builder()
      .issuer(jwtProperties.issuer())
      .subject(user.getId().toString())
      .issuedAt(issuedAt)
      .expiresAt(expiresAt)
      .claim("roles", user.getRoles().stream().map(UserRole::name).toList())
      .build();

    var encodedToken = jwtEncoder.encode(JwtEncoderParameters.from(claims));

    return new GeneratedToken(encodedToken.getTokenValue(), jwtProperties.accessTokenTtl().toSeconds());
  }

  public record GeneratedToken(
    String rawValue,
    long expiresIn) {
  }
}
