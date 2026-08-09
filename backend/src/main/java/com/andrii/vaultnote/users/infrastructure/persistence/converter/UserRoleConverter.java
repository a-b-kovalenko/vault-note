package com.andrii.vaultnote.users.infrastructure.persistence.converter;

import static java.util.Objects.isNull;

import com.andrii.vaultnote.users.domain.UserRole;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class UserRoleConverter implements AttributeConverter<UserRole, Short> {

  @Override
  public Short convertToDatabaseColumn(UserRole role) {
    return isNull(role) ? null : role.code();
  }

  @Override
  public UserRole convertToEntityAttribute(Short code) {
    return isNull(code) ? null : UserRole.fromCode(code);
  }
}
