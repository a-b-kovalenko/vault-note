package com.andrii.vaultnote.app.security.csrf;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.Objects;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * Generates and verifies short-lived, signed CSRF tokens.
 *
 * <p>
 * The token contains no user data. Its expiry and random nonce are signed with
 * HMAC-SHA-256, so the token can be verified without server-side state.
 */
public final class StatelessCsrfTokenService {

  private static final String HMAC_ALGORITHM = "HmacSHA256";
  private static final String TOKEN_VERSION = "v1";
  private static final int TOKEN_PARTS = 4;
  private static final int NONCE_BYTES = 32;
  private static final int SIGNATURE_BYTES = 32;
  private static final int MIN_SIGNING_KEY_BYTES = 32;

  private final SecretKey signingKey;
  private final Duration tokenTtl;
  private final Clock clock;
  private final SecureRandom secureRandom;

  public StatelessCsrfTokenService(
    SecretKey signingKey,
    Duration tokenTtl,
    Clock clock) {
    this(signingKey, tokenTtl, clock, new SecureRandom());
  }

  StatelessCsrfTokenService(
    SecretKey signingKey,
    Duration tokenTtl,
    Clock clock,
    SecureRandom secureRandom) {
    this.signingKey = normalizeSigningKey(signingKey);
    this.tokenTtl = validateTokenTtl(tokenTtl);
    this.clock = Objects.requireNonNull(clock, "clock");
    this.secureRandom = Objects.requireNonNull(secureRandom, "secureRandom");
  }

  public String generateToken() {
    var expiresAt = clock.instant().plus(tokenTtl).getEpochSecond();
    var nonce = new byte[NONCE_BYTES];
    secureRandom.nextBytes(nonce);
    var encodedNonce = encode(nonce);
    var payload = String.join(".", TOKEN_VERSION, Long.toString(expiresAt), encodedNonce);

    return payload + "." + encode(sign(payload));
  }

  public boolean isValid(String token) {
    if (token == null || token.isBlank()) {
      return false;
    }

    var parts = token.split("\\.", -1);
    if (parts.length != TOKEN_PARTS || !TOKEN_VERSION.equals(parts[0])
      || parts[1].isBlank() || parts[2].isBlank() || parts[3].isBlank()) {
      return false;
    }

    long expiresAt;
    byte[] nonce;
    byte[] signature;
    try {
      expiresAt = Long.parseLong(parts[1]);
      nonce = decode(parts[2]);
      signature = decode(parts[3]);
    } catch (IllegalArgumentException exception) {
      return false;
    }

    if (nonce.length != NONCE_BYTES || signature.length != SIGNATURE_BYTES) {
      return false;
    }

    var payload = String.join(".", parts[0], parts[1], parts[2]);
    var expectedSignature = sign(payload);
    return MessageDigest.isEqual(expectedSignature, signature)
      && clock.instant().getEpochSecond() < expiresAt;
  }

  private byte[] sign(String payload) {
    try {
      var mac = Mac.getInstance(HMAC_ALGORITHM);
      mac.init(signingKey);
      return mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
    } catch (GeneralSecurityException exception) {
      throw new IllegalStateException("Unable to sign CSRF token", exception);
    }
  }

  private static SecretKey normalizeSigningKey(SecretKey key) {
    Objects.requireNonNull(key, "signingKey");
    var encodedKey = key.getEncoded();
    if (encodedKey == null || encodedKey.length < MIN_SIGNING_KEY_BYTES) {
      throw new IllegalArgumentException("CSRF signing key must contain at least 32 bytes");
    }
    return new SecretKeySpec(encodedKey.clone(), HMAC_ALGORITHM);
  }

  private static Duration validateTokenTtl(Duration ttl) {
    Objects.requireNonNull(ttl, "tokenTtl");
    if (ttl.isNegative() || ttl.isZero() || ttl.toSeconds() <= 0) {
      throw new IllegalArgumentException("CSRF token TTL must contain at least one second");
    }
    return ttl;
  }

  private static String encode(byte[] value) {
    return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
  }

  private static byte[] decode(String value) {
    return Base64.getUrlDecoder().decode(value);
  }
}
