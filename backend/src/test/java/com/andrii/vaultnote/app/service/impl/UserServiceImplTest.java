package com.andrii.vaultnote.app.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import com.andrii.vaultnote.app.api.users.dto.UserInfoDto;
import com.andrii.vaultnote.app.mapper.UserMapper;
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

import java.util.List;

@ExtendWith(MockitoExtension.class)
@FieldDefaults(level = AccessLevel.PRIVATE)
class UserServiceImplTest {

  @Mock
  UserJpaRepository userRepository;

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
}
