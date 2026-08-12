package com.andrii.vaultnote.app.service.impl;

import com.andrii.vaultnote.app.api.auth.dto.RegisterUserRequest;
import com.andrii.vaultnote.app.api.auth.dto.RegisterUserResponse;
import com.andrii.vaultnote.app.exception.EntityExistsException;
import com.andrii.vaultnote.app.mapper.UserMapper;
import com.andrii.vaultnote.app.service.EmailVerificationService;
import com.andrii.vaultnote.users.domain.UserRole;
import com.andrii.vaultnote.users.infrastructure.persistence.entity.UserEntity;
import com.andrii.vaultnote.users.infrastructure.persistence.repository.UserJpaRepository;
import java.sql.SQLException;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@FieldDefaults(level = AccessLevel.PRIVATE)
class RegistrationServiceImplTest {

  private static final String EMAIL = "new@email.com";
  private static final String DISPLAY_NAME = "John Doe";
  private static final String RAW_PASSWORD = "rawPassword123";
  private static final String MOCK_HASH = "stubbed_password_hash";

  @Mock
  UserJpaRepository userJpaRepository;
  @Mock
  PasswordEncoder passwordEncoder;
  @Mock
  EmailVerificationService emailVerificationService;

  @Spy
  UserMapper userMapper = Mappers.getMapper(UserMapper.class);

  @InjectMocks
  RegistrationServiceImpl registrationService;

  @Test
  void shouldRegisterUser() {
    var request = RegisterUserRequest.builder()
      .email(EMAIL)
      .displayName(DISPLAY_NAME)
      .password(RAW_PASSWORD)
      .build();

    when(userJpaRepository.existsByEmail(EMAIL)).thenReturn(false);
    when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(MOCK_HASH);

    when(userJpaRepository.save(any(UserEntity.class))).thenAnswer(invocation -> {
      var userToSave = (UserEntity) invocation.getArgument(0);
      return userToSave.toBuilder()
        .id(1L)
        .build();
    });

    var response = registrationService.registerUser(request);

    verify(passwordEncoder, times(1)).encode(RAW_PASSWORD);
    verify(userMapper).toUserEntity(request, MOCK_HASH);
    var userCaptor = ArgumentCaptor.forClass(UserEntity.class);
    verify(userJpaRepository).save(userCaptor.capture());
    verify(emailVerificationService).issueVerificationEmail(any(UserEntity.class));

    assertThat(userCaptor.getValue().getRoles()).containsExactly(UserRole.USER);
    assertThat(response)
      .extracting(RegisterUserResponse::userId)
      .isEqualTo(1L);
  }

  @Test
  void shouldThrowIfEmailExists() {
    var request = RegisterUserRequest.builder()
      .email(EMAIL)
      .displayName(DISPLAY_NAME)
      .password("password")
      .build();
    when(userJpaRepository.existsByEmail(EMAIL)).thenReturn(true);

    assertThatExceptionOfType(EntityExistsException.class)
      .isThrownBy(() -> registrationService.registerUser(request))
      .withMessage("UserEntity with email '" + EMAIL + "' already exists.");

    verifyNoInteractions(userMapper);
    verifyNoInteractions(passwordEncoder);
    verifyNoInteractions(emailVerificationService);
    verify(userJpaRepository, never()).save(any(UserEntity.class));
  }

  @Test
  void shouldTranslateIntegrityViolationToEntityExists() {
    var request = RegisterUserRequest.builder()
      .email(EMAIL)
      .displayName(DISPLAY_NAME)
      .password(RAW_PASSWORD)
      .build();

    when(userJpaRepository.existsByEmail(EMAIL)).thenReturn(false);
    when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(MOCK_HASH);
    when(userJpaRepository.save(any(UserEntity.class)))
      .thenThrow(new DataIntegrityViolationException("Unique constraint violation",
        new SQLException("duplicate key value violates unique constraint \"uk_users_email\"",
          "23505")));

    assertThatExceptionOfType(EntityExistsException.class)
      .isThrownBy(() -> registrationService.registerUser(request))
      .withMessage("UserEntity with email '" + EMAIL + "' already exists.");
  }

  @Test
  void shouldRethrowNonUniqueIntegrityViolation() {
    var request = RegisterUserRequest.builder()
      .email(EMAIL)
      .displayName(DISPLAY_NAME)
      .password(RAW_PASSWORD)
      .build();

    when(userJpaRepository.existsByEmail(EMAIL)).thenReturn(false);
    when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(MOCK_HASH);
    when(userJpaRepository.save(any(UserEntity.class)))
      .thenThrow(new DataIntegrityViolationException("Not null violation",
        new SQLException("null value in column \"password_hash\" violates not-null constraint",
          "23502")));

    assertThatExceptionOfType(DataIntegrityViolationException.class)
      .isThrownBy(() -> registrationService.registerUser(request))
      .withMessage("Not null violation");
  }
}
