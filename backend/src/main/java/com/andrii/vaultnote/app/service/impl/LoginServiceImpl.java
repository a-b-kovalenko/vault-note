package com.andrii.vaultnote.app.service.impl;

import com.andrii.vaultnote.app.api.auth.dto.LoginRequest;
import com.andrii.vaultnote.app.config.RefreshTokenProperties;
import com.andrii.vaultnote.app.exception.AuthenticationFailedException;
import com.andrii.vaultnote.app.security.SecureTokenGenerator;
import com.andrii.vaultnote.app.service.AuthenticationResultFactory;
import com.andrii.vaultnote.app.service.LoginResult;
import com.andrii.vaultnote.app.service.LoginService;
import com.andrii.vaultnote.users.infrastructure.persistence.entity.RefreshTokenEntity;
import com.andrii.vaultnote.users.infrastructure.persistence.entity.UserEntity;
import com.andrii.vaultnote.users.infrastructure.persistence.repository.RefreshTokenJpaRepository;
import com.andrii.vaultnote.users.infrastructure.persistence.repository.UserJpaRepository;
import java.time.Clock;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LoginServiceImpl implements LoginService {

  SecureTokenGenerator secureTokenGenerator;
  AuthenticationResultFactory authenticationResultFactory;
  RefreshTokenJpaRepository refreshTokenRepository;
  RefreshTokenProperties refreshTokenProperties;
  UserJpaRepository userRepository;
  PasswordEncoder passwordEncoder;
  Clock clock;

  @Override
  @Transactional
  public LoginResult login(LoginRequest request) {
    log.info("Received request to login user");

    var user = userRepository.findByEmail(request.email())
        .filter(UserEntity::isEmailVerified)
        .filter(userEntity -> passwordEncoder.matches(
            request.password(), userEntity.getPasswordHash()))
        .orElseThrow(AuthenticationFailedException::new);

    var generatedRefreshToken = secureTokenGenerator.generate();
    var refreshToken = RefreshTokenEntity.builder()
        .user(user)
        .tokenHash(generatedRefreshToken.hash())
        .tokenFamilyId(UUID.randomUUID())
        .expiresAt(clock.instant().plus(refreshTokenProperties.ttl()))
        .build();
    refreshTokenRepository.save(refreshToken);

    return authenticationResultFactory.create(user, generatedRefreshToken.rawValue());
  }
}
