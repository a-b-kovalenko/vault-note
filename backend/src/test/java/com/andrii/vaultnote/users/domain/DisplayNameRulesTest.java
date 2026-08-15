package com.andrii.vaultnote.users.domain;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

class DisplayNameRulesTest {

  @Test
  void shouldAcceptValidDisplayName() {
    assertThatCode(() -> DisplayNameRules.validate("Andrii Kovalenko"))
      .doesNotThrowAnyException();
  }

  @Test
  void shouldRejectBlankDisplayName() {
    assertThatIllegalArgumentException()
      .isThrownBy(() -> DisplayNameRules.validate(" "))
      .withMessage("Display name must not be blank.");
  }

  @Test
  void shouldRejectDisplayNameLongerThan100Characters() {
    assertThatIllegalArgumentException()
      .isThrownBy(() -> DisplayNameRules.validate("a".repeat(DisplayNameRules.MAX_LENGTH + 1)))
      .withMessage("Display name must not exceed 100 characters.");
  }
}
