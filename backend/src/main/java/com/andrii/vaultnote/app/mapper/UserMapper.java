package com.andrii.vaultnote.app.mapper;

import com.andrii.vaultnote.app.api.auth.dto.RegisterUserRequest;
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
}
