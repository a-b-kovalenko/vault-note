package com.andrii.vaultnote.app.security.oauth2;

import com.andrii.vaultnote.app.config.OAuth2AuthorizationRequestCookieProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.util.StringUtils;

/**
 * Stores the OAuth authorization request in a short-lived encrypted cookie.
 *
 * <p>
 * The cookie keeps the OAuth flow stateless while protecting the state and PKCE
 * verifier from client-side inspection. The existing application JWT secret is
 * domain-separated before it is used as the cookie-encryption key.
 */
public class CookieOAuth2AuthorizationRequestRepository
  implements
    AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

  private static final String CALLBACK_COOKIE_PATH = "/login/oauth2/code";
  private static final String KEY_DERIVATION_CONTEXT = "vaultnote-oauth2-authorization-request-cookie-v1";
  private static final String CIPHER_ALGORITHM = "AES/GCM/NoPadding";
  private static final int GCM_IV_LENGTH_BYTES = 12;
  private static final int GCM_TAG_LENGTH_BITS = 128;
  private static final int MAX_COOKIE_VALUE_LENGTH = 4096;

  private final ObjectMapper objectMapper;
  private final OAuth2AuthorizationRequestCookieProperties properties;
  private final SecretKey encryptionKey;
  private final SecureRandom secureRandom;

  public CookieOAuth2AuthorizationRequestRepository(
    ObjectMapper objectMapper,
    SecretKey applicationSecret,
    OAuth2AuthorizationRequestCookieProperties properties) {
    this.objectMapper = Objects.requireNonNull(objectMapper);
    this.properties = Objects.requireNonNull(properties);
    this.encryptionKey = deriveEncryptionKey(Objects.requireNonNull(applicationSecret));
    this.secureRandom = new SecureRandom();
    if (properties.maxAge() == null || properties.maxAge().isNegative()
      || properties.maxAge().isZero()) {
      throw new IllegalArgumentException("OAuth2 authorization request cookie max age must be positive");
    }
  }

  @Override
  public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
    return findCookie(request)
      .map(Cookie::getValue)
      .map(this::decrypt)
      .orElse(null);
  }

  @Override
  public void saveAuthorizationRequest(
    OAuth2AuthorizationRequest authorizationRequest,
    HttpServletRequest request,
    HttpServletResponse response) {
    if (authorizationRequest == null) {
      clearCookie(response);
      return;
    }

    var expiresAt = Instant.now().plus(properties.maxAge()).getEpochSecond();
    var payload = CookiePayload.from(authorizationRequest, expiresAt);
    var encryptedPayload = encrypt(payload);
    if (encryptedPayload.length() > MAX_COOKIE_VALUE_LENGTH) {
      throw new IllegalStateException("OAuth2 authorization request cookie is too large");
    }

    response.addHeader(HttpHeaders.SET_COOKIE, createCookie(encryptedPayload).toString());
  }

  @Override
  public OAuth2AuthorizationRequest removeAuthorizationRequest(
    HttpServletRequest request,
    HttpServletResponse response) {
    var authorizationRequest = loadAuthorizationRequest(request);
    clearCookie(response);
    return authorizationRequest;
  }

  private java.util.Optional<Cookie> findCookie(HttpServletRequest request) {
    if (request == null || request.getCookies() == null) {
      return java.util.Optional.empty();
    }
    for (var cookie : request.getCookies()) {
      if (properties.cookieName().equals(cookie.getName())) {
        return java.util.Optional.of(cookie);
      }
    }
    return java.util.Optional.empty();
  }

  private String encrypt(CookiePayload payload) {
    try {
      var initializationVector = new byte[GCM_IV_LENGTH_BYTES];
      secureRandom.nextBytes(initializationVector);

      var cipher = Cipher.getInstance(CIPHER_ALGORITHM);
      cipher.init(
        Cipher.ENCRYPT_MODE,
        encryptionKey,
        new GCMParameterSpec(GCM_TAG_LENGTH_BITS, initializationVector));
      var ciphertext = cipher.doFinal(objectMapper.writeValueAsBytes(payload));
      var combined = ByteBuffer.allocate(initializationVector.length + ciphertext.length)
        .put(initializationVector)
        .put(ciphertext)
        .array();
      return Base64.getUrlEncoder().withoutPadding().encodeToString(combined);
    } catch (GeneralSecurityException | IOException exception) {
      throw new IllegalStateException("Unable to encrypt OAuth2 authorization request", exception);
    }
  }

  private OAuth2AuthorizationRequest decrypt(String encodedPayload) {
    if (!StringUtils.hasText(encodedPayload)
      || encodedPayload.length() > MAX_COOKIE_VALUE_LENGTH) {
      return null;
    }

    try {
      var encryptedPayload = Base64.getUrlDecoder().decode(encodedPayload);
      if (encryptedPayload.length <= GCM_IV_LENGTH_BYTES) {
        return null;
      }

      var initializationVector = new byte[GCM_IV_LENGTH_BYTES];
      var ciphertext = new byte[encryptedPayload.length - GCM_IV_LENGTH_BYTES];
      var buffer = ByteBuffer.wrap(encryptedPayload);
      buffer.get(initializationVector);
      buffer.get(ciphertext);

      var cipher = Cipher.getInstance(CIPHER_ALGORITHM);
      cipher.init(
        Cipher.DECRYPT_MODE,
        encryptionKey,
        new GCMParameterSpec(GCM_TAG_LENGTH_BITS, initializationVector));
      var payload = objectMapper.readValue(cipher.doFinal(ciphertext), CookiePayload.class);
      if (payload.expiresAt() <= Instant.now().getEpochSecond()) {
        return null;
      }
      return payload.toAuthorizationRequest();
    } catch (GeneralSecurityException | IOException
      | IllegalArgumentException exception) {
      return null;
    }
  }

  private ResponseCookie createCookie(String value) {
    return ResponseCookie.from(properties.cookieName(), value)
      .httpOnly(true)
      .secure(properties.secure())
      .sameSite("Lax")
      .path(CALLBACK_COOKIE_PATH)
      .maxAge(properties.maxAge())
      .build();
  }

  private void clearCookie(HttpServletResponse response) {
    response.addHeader(
      HttpHeaders.SET_COOKIE,
      ResponseCookie.from(properties.cookieName(), "")
        .httpOnly(true)
        .secure(properties.secure())
        .sameSite("Lax")
        .path(CALLBACK_COOKIE_PATH)
        .maxAge(java.time.Duration.ZERO)
        .build()
        .toString());
  }

  private static SecretKey deriveEncryptionKey(SecretKey applicationSecret) {
    try {
      var digest = MessageDigest.getInstance("SHA-256");
      digest.update(KEY_DERIVATION_CONTEXT.getBytes(StandardCharsets.UTF_8));
      digest.update((byte) 0);
      digest.update(applicationSecret.getEncoded());
      return new SecretKeySpec(digest.digest(), "AES");
    } catch (GeneralSecurityException exception) {
      throw new IllegalStateException("Unable to derive OAuth2 cookie encryption key", exception);
    }
  }

  private record CookiePayload(
    String authorizationUri,
    String clientId,
    String redirectUri,
    Set<String> scopes,
    String state,
    Map<String, Object> additionalParameters,
    Map<String, Object> attributes,
    long expiresAt) {

    private CookiePayload {
      scopes = scopes == null ? Set.of() : Set.copyOf(scopes);
      additionalParameters = immutableCopy(additionalParameters);
      attributes = immutableCopy(attributes);
    }

    private static CookiePayload from(
      OAuth2AuthorizationRequest authorizationRequest,
      long expiresAt) {
      return new CookiePayload(
        authorizationRequest.getAuthorizationUri(),
        authorizationRequest.getClientId(),
        authorizationRequest.getRedirectUri(),
        authorizationRequest.getScopes(),
        authorizationRequest.getState(),
        authorizationRequest.getAdditionalParameters(),
        authorizationRequest.getAttributes(),
        expiresAt);
    }

    private OAuth2AuthorizationRequest toAuthorizationRequest() {
      var registrationId = attributes.get(OAuth2ParameterNames.REGISTRATION_ID);
      if (!(registrationId instanceof String)
        || !StringUtils.hasText((String) registrationId)
        || !StringUtils.hasText(authorizationUri)
        || !StringUtils.hasText(clientId)
        || !StringUtils.hasText(redirectUri)
        || !StringUtils.hasText(state)) {
        return null;
      }

      return OAuth2AuthorizationRequest.authorizationCode()
        .authorizationUri(authorizationUri)
        .clientId(clientId)
        .redirectUri(redirectUri)
        .scopes(scopes)
        .state(state)
        .additionalParameters(parameters -> parameters.putAll(additionalParameters))
        .attributes(attributes -> attributes.putAll(this.attributes))
        .build();
    }

    private static Map<String, Object> immutableCopy(Map<String, Object> source) {
      if (source == null || source.isEmpty()) {
        return Map.of();
      }
      return Collections.unmodifiableMap(new LinkedHashMap<>(source));
    }
  }
}
