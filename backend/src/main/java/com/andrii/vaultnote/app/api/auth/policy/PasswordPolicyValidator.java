package com.andrii.vaultnote.app.api.auth.policy;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordPolicyValidator implements ConstraintValidator<PasswordPolicy, String> {

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    if (value == null || value.isBlank()) {
      return true;
    }
    var hasTwoDigits = value.codePoints()
      .filter(Character::isDigit)
      .limit(2)
      .count() == 2;

    var hasAlphabeticCharacter = value.codePoints()
      .anyMatch(Character::isAlphabetic);

    return hasTwoDigits && hasAlphabeticCharacter;
  }
}
