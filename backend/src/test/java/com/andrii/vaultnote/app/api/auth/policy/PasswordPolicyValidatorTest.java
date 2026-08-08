package com.andrii.vaultnote.app.api.auth.policy;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class PasswordPolicyValidatorTest {

  private final PasswordPolicyValidator validator = new PasswordPolicyValidator();

  @ParameterizedTest
  @ValueSource(strings = {"Password1234", "password1234", "пароль12"})
  void shouldAcceptPasswordWithTwoDigitsAndAlphabeticCharacter(String password) {
    assertThat(validator.isValid(password, null)).isTrue();
  }

  @ParameterizedTest
  @ValueSource(strings = {"Password1", "123456789012", "Password"})
  void shouldRejectPasswordWithoutRequiredCharacters(String password) {
    assertThat(validator.isValid(password, null)).isFalse();
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" ", "\t"})
  void shouldIgnoreNullAndBlankValues(String password) {
    assertThat(validator.isValid(password, null)).isTrue();
  }
}
