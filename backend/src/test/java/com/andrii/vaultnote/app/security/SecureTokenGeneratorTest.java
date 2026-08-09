package com.andrii.vaultnote.app.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SecureTokenGeneratorTest {

  private final SecureTokenGenerator generator = new SecureTokenGenerator();

  @Test
  void shouldGenerateUrlSafeTokenAndHash() {
    var generatedToken = generator.generate();

    assertThat(generatedToken.rawValue())
        .hasSize(43)
        .matches("[A-Za-z0-9_-]+");
    assertThat(generatedToken.hash())
        .hasSize(64)
        .isEqualTo(generator.hash(generatedToken.rawValue()));
    assertThat(generatedToken.hash()).isNotEqualTo(generatedToken.rawValue());
  }

  @Test
  void shouldGenerateDifferentTokens() {
    var firstToken = generator.generate();
    var secondToken = generator.generate();

    assertThat(secondToken.rawValue()).isNotEqualTo(firstToken.rawValue());
    assertThat(secondToken.hash()).isNotEqualTo(firstToken.hash());
  }
}
