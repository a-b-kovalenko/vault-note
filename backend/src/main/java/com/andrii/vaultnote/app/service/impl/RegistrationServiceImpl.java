package com.andrii.vaultnote.app.service.impl;

import com.andrii.vaultnote.app.api.auth.dto.RegisterUserRequest;
import com.andrii.vaultnote.app.api.auth.dto.RegisterUserResponse;
import com.andrii.vaultnote.app.exception.EntityExistsException;
import com.andrii.vaultnote.app.mapper.UserMapper;
import com.andrii.vaultnote.app.security.SecureTokenGenerator;
import com.andrii.vaultnote.app.service.EmailVerificationService;
import com.andrii.vaultnote.app.service.RegistrationService;
import com.andrii.vaultnote.users.domain.UserRole;
import com.andrii.vaultnote.users.infrastructure.persistence.entity.UserEntity;
import com.andrii.vaultnote.users.infrastructure.persistence.repository.UserJpaRepository;
import jakarta.transaction.Transactional;
import java.sql.SQLException;
import java.util.EnumSet;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RegistrationServiceImpl implements RegistrationService {

  private static final String SQLSTATE_UNIQUE_VIOLATION = "23505";

  UserJpaRepository userJpaRepository;

  UserMapper userMapper;
  PasswordEncoder passwordEncoder;
  EmailVerificationService emailVerificationService;
  SecureTokenGenerator tokenGenerator;

  @Override
  @Transactional
  public RegisterUserResponse registerUser(RegisterUserRequest request) {
    log.info("Received request to register user");
    var email = request.email();
    if (userJpaRepository.existsByEmail(email)) {
      throw new EntityExistsException(UserEntity.class.getSimpleName(), "email", email);
    }
    var passwordHash = passwordEncoder.encode(request.password());
    var newEntity = userMapper.toUserEntity(request, passwordHash).toBuilder()
        .roles(EnumSet.of(UserRole.USER))
        .build();

    UserEntity savedEntity;
    try {
      savedEntity = userJpaRepository.save(newEntity);
    } catch (DataIntegrityViolationException e) {
      if (isUniqueViolation(e)) {
        throw new EntityExistsException(UserEntity.class.getSimpleName(), "email", email);
      }
      throw e;
    }

    emailVerificationService.issueVerificationEmail(savedEntity);

    return new RegisterUserResponse(savedEntity.getId());
  }

  private static boolean isUniqueViolation(DataIntegrityViolationException e) {
    var cause = e.getMostSpecificCause();
    return cause instanceof SQLException sqlException
        && SQLSTATE_UNIQUE_VIOLATION.equals(sqlException.getSQLState());
  }
}
