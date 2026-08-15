package com.andrii.vaultnote.app.mapper;

import com.andrii.vaultnote.app.api.auth.dto.RegisterUserRequest;
import com.andrii.vaultnote.app.api.users.dto.UserInfoDto;
import com.andrii.vaultnote.app.api.users.dto.UserProfileDto;
import com.andrii.vaultnote.users.domain.UserRole;
import com.andrii.vaultnote.users.infrastructure.persistence.entity.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "emailVerified", ignore = true)
  @Mapping(target = "roles", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  UserEntity toUserEntity(RegisterUserRequest request, String passwordHash);

  UserInfoDto toUserInfoDto(UserEntity userEntity);

  default UserProfileDto toUserProfileDto(UserEntity userEntity) {
    return UserProfileDto.builder()
      .id(userEntity.getId())
      .email(userEntity.getEmail())
      .displayName(userEntity.getDisplayName())
      .emailVerified(userEntity.isEmailVerified())
      .roles(userEntity.getRoles().stream().map(UserRole::name).sorted().toList())
      .build();
  }
}
