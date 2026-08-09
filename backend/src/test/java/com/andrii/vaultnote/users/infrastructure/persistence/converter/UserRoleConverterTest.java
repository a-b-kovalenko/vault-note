package com.andrii.vaultnote.users.infrastructure.persistence.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.andrii.vaultnote.users.domain.UserRole;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.junit.jupiter.api.Test;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
class UserRoleConverterTest {

  UserRoleConverter converter = new UserRoleConverter();

  @Test
  void shouldConvertRoleToNumericCode() {
    assertThat(converter.convertToDatabaseColumn(UserRole.USER)).isEqualTo((short) 1);
    assertThat(converter.convertToDatabaseColumn(UserRole.ADMIN)).isEqualTo((short) 2);
  }

  @Test
  void shouldConvertNumericCodeToRole() {
    assertThat(converter.convertToEntityAttribute((short) 1)).isEqualTo(UserRole.USER);
    assertThat(converter.convertToEntityAttribute((short) 2)).isEqualTo(UserRole.ADMIN);
  }

  @Test
  void shouldReturnNullForNullAttributeValues() {
    assertThat(converter.convertToDatabaseColumn(null)).isNull();
    assertThat(converter.convertToEntityAttribute(null)).isNull();
  }

  @Test
  void shouldRejectUnknownRoleCode() {
    assertThatThrownBy(() -> converter.convertToEntityAttribute((short) 99))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Unknown user role code: 99");
  }
}
