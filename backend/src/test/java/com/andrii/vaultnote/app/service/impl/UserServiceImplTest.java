package com.andrii.vaultnote.app.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.andrii.vaultnote.app.api.users.dto.UserProfileDto;
import com.andrii.vaultnote.app.exception.UserNotFoundException;
import com.andrii.vaultnote.app.api.users.dto.UserInfoDto;
import com.andrii.vaultnote.app.mapper.UserMapper;
import com.andrii.vaultnote.app.security.CurrentUserProvider;
import com.andrii.vaultnote.users.domain.UserRole;
import com.andrii.vaultnote.users.infrastructure.persistence.entity.UserEntity;
import com.andrii.vaultnote.users.infrastructure.persistence.repository.UserJpaRepository;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mapstruct.factory.Mappers;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
@FieldDefaults(level = AccessLevel.PRIVATE)
class UserServiceImplTest {

  @Mock
  UserJpaRepository userRepository;

  @Mock
  CurrentUserProvider currentUserProvider;

  @Spy
  UserMapper userMapper = Mappers.getMapper(UserMapper.class);

  @InjectMocks
  UserServiceImpl userService;

  @Test
  void shouldReturnMappedUsers() {
    var pageable = PageRequest.of(0, 20);
    var user = UserEntity.builder()
      .id(1L)
      .email("user@example.com")
      .displayName("User")
      .build();
    Page<UserEntity> users = new PageImpl<>(List.of(user), pageable, 1);

    when(userRepository.findAll(pageable)).thenReturn(users);

    var result = userService.getUsers(pageable);

    assertThat(result.getContent())
      .hasSize(1)
      .extracting(UserInfoDto::id, UserInfoDto::email, UserInfoDto::displayName)
      .containsExactly(tuple(1L, "user@example.com", "User"));

    assertThat(result.getTotalElements()).isEqualTo(1);

    verify(userRepository).findAll(pageable);
    verify(userMapper).toUserInfoDto(user);
  }

  @Test
  void shouldReturnCurrentUserProfile() {
    var user = UserEntity.builder()
      .id(1L)
      .email("user@example.com")
      .displayName("User")
      .emailVerified(false)
      .roles(EnumSet.of(UserRole.USER))
      .build();

    when(currentUserProvider.currentUserId()).thenReturn(1L);
    when(userRepository.findUserWithRolesById(1L)).thenReturn(Optional.of(user));

    var result = userService.getCurrentProfile();

    assertThat(result)
      .extracting(
        UserProfileDto::id,
        UserProfileDto::email,
        UserProfileDto::displayName,
        UserProfileDto::emailVerified)
      .containsExactly(1L, "user@example.com", "User", false);
    assertThat(result.roles()).containsExactly("USER");
    verify(userRepository).findUserWithRolesById(1L);
  }

  @Test
  void shouldUpdateCurrentUserDisplayName() {
    var user = UserEntity.builder()
      .id(1L)
      .email("user@example.com")
      .displayName("User")
      .emailVerified(true)
      .roles(EnumSet.of(UserRole.USER))
      .build();

    when(currentUserProvider.currentUserId()).thenReturn(1L);
    when(userRepository.findUserWithRolesById(1L)).thenReturn(Optional.of(user));
    when(userRepository.saveAndFlush(user)).thenReturn(user);

    var result = userService.updateCurrentProfile("Updated User");

    assertThat(user.getDisplayName()).isEqualTo("Updated User");
    assertThat(result.displayName()).isEqualTo("Updated User");
    assertThat(result.email()).isEqualTo("user@example.com");
    assertThat(result.emailVerified()).isTrue();
    verify(userRepository).saveAndFlush(user);
  }

  @Test
  void shouldRejectBlankDisplayName() {
    assertThatIllegalArgumentException()
      .isThrownBy(() -> userService.updateCurrentProfile(" "))
      .withMessage("Display name must not be blank.");
  }

  @Test
  void shouldRejectDisplayNameLongerThan100Characters() {
    assertThatIllegalArgumentException()
      .isThrownBy(() -> userService.updateCurrentProfile("a".repeat(101)))
      .withMessage("Display name must not exceed 100 characters.");
  }

  @Test
  void shouldFailWhenCurrentUserDoesNotExist() {
    when(currentUserProvider.currentUserId()).thenReturn(1L);
    when(userRepository.findUserWithRolesById(1L)).thenReturn(Optional.empty());

    assertThatThrownBy(userService::getCurrentProfile)
      .isInstanceOf(UserNotFoundException.class)
      .hasMessage("User with id '1' was not found.");
  }
}
