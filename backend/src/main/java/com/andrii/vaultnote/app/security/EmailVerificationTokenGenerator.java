package com.andrii.vaultnote.app.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class EmailVerificationTokenGenerator {

  private static final int TOKEN_BYTES = 32;
  private static final String HASH_ALGORITHM = "SHA-256";

  private final SecureRandom secureRandom = new SecureRandom();

  public GeneratedToken generate() {
    var randomBytes = new byte[TOKEN_BYTES];
    secureRandom.nextBytes(randomBytes);

    var rawValue = Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(randomBytes);

    return new GeneratedToken(rawValue, hash(rawValue));
  }

  public String hash(String rawValue) {
    Objects.requireNonNull(rawValue, "rawValue");

    try {
      var digest = MessageDigest.getInstance(HASH_ALGORITHM);
      var encodedValue = rawValue.getBytes(StandardCharsets.UTF_8);
      return HexFormat.of().formatHex(digest.digest(encodedValue));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("Required hash algorithm is unavailable", exception);
    }
  }

  public record GeneratedToken(
      String rawValue,
      String hash) {
  }
}
