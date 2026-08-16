package com.andrii.vaultnote.app.api.security;

import static com.andrii.vaultnote.app.api.security.GoogleOAuth2TestConfiguration.GOOGLE_NONCE;
import static com.andrii.vaultnote.app.api.security.GoogleOAuth2TestConfiguration.GOOGLE_PICTURE_URL;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.andrii.vaultnote.app.security.oauth2.GoogleAvatarImageClient;
import com.andrii.vaultnote.support.AbstractBaseIntegrationTest;
import com.andrii.vaultnote.users.domain.OAuthProvider;
import com.andrii.vaultnote.users.infrastructure.persistence.repository.OAuthIdentityJpaRepository;
import com.andrii.vaultnote.users.infrastructure.persistence.repository.UserAvatarJpaRepository;
import com.andrii.vaultnote.users.infrastructure.persistence.repository.UserJpaRepository;
import com.github.database.rider.core.api.dataset.DataSet;
import com.github.tomakehurst.wiremock.WireMockServer;
import io.restassured.response.Response;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@DataSet(value = "auth-baseline.yml", skipCleaningFor = {"databasechangelog", "databasechangeloglock"})
@Import(GoogleOAuth2TestConfiguration.class)
class GoogleOAuth2IntegrationTest extends AbstractBaseIntegrationTest {

  private static final String AUTHORIZATION_ENDPOINT = "/oauth2/authorization/google";
  private static final String CALLBACK_ENDPOINT = "/login/oauth2/code/google";
  private static final String AUTHORIZATION_COOKIE = "vaultnote_oauth2_authorization_request";
  private static final String REFRESH_COOKIE = "vaultnote_refresh_token";
  private static final String GOOGLE_EMAIL = "wiremock-google@example.com";
  private static final String GOOGLE_NO_PICTURE_EMAIL = "wiremock-no-picture@example.com";
  private static final String GOOGLE_SUBJECT = "wiremock-google-subject";
  private static final String MOCK_AUTHORIZATION_CODE = "mock-authorization-code";
  private static final String MOCK_NO_PICTURE_AUTHORIZATION_CODE = "mock-no-picture-code";
  private static final String GOOGLE_PROVIDER_PROPERTY_PREFIX = "spring.security.oauth2.client.provider.google.";
  private static final String GOOGLE_AUTHORIZATION_URI_PROPERTY = GOOGLE_PROVIDER_PROPERTY_PREFIX + "authorization-uri";
  private static final String GOOGLE_TOKEN_URI_PROPERTY = GOOGLE_PROVIDER_PROPERTY_PREFIX + "token-uri";
  private static final String GOOGLE_JWK_SET_URI_PROPERTY = GOOGLE_PROVIDER_PROPERTY_PREFIX + "jwk-set-uri";
  private static final String GOOGLE_USER_INFO_URI_PROPERTY = GOOGLE_PROVIDER_PROPERTY_PREFIX + "user-info-uri";
  private static final String GOOGLE_USER_NAME_ATTRIBUTE_PROPERTY = GOOGLE_PROVIDER_PROPERTY_PREFIX
    + "user-name-attribute";
  private static final String GOOGLE_ISSUER_URI_PROPERTY = GOOGLE_PROVIDER_PROPERTY_PREFIX + "issuer-uri";
  private static final String GOOGLE_AUTHORIZATION_PATH = "/oauth2/authorize";
  private static final String GOOGLE_TOKEN_PATH = "/oauth2/token";
  private static final String GOOGLE_JWK_SET_PATH = "/oauth2/jwks";
  private static final String GOOGLE_USER_INFO_PATH = "/userinfo";
  private static final String GOOGLE_USER_NAME_ATTRIBUTE = "sub";
  private static final String GOOGLE_ISSUER_URI = "https://accounts.google.com";

  private static final WireMockServer GOOGLE_PROVIDER = startGoogleProvider();

  UserJpaRepository userRepository;
  OAuthIdentityJpaRepository oauthIdentityRepository;
  UserAvatarJpaRepository avatarRepository;
  GoogleAvatarImageClient googleAvatarImageClient;

  @Autowired
  GoogleOAuth2IntegrationTest(
    UserJpaRepository userRepository,
    OAuthIdentityJpaRepository oauthIdentityRepository,
    UserAvatarJpaRepository avatarRepository,
    GoogleAvatarImageClient googleAvatarImageClient) {
    this.userRepository = userRepository;
    this.oauthIdentityRepository = oauthIdentityRepository;
    this.avatarRepository = avatarRepository;
    this.googleAvatarImageClient = googleAvatarImageClient;
  }

  @AfterAll
  static void stopGoogleProvider() {
    GOOGLE_PROVIDER.stop();
  }

  @BeforeEach
  void clearAvatarClientInvocations() {
    clearInvocations(googleAvatarImageClient);
  }

  @DynamicPropertySource
  static void registerGoogleProviderProperties(DynamicPropertyRegistry registry) {
    registry.add(GOOGLE_AUTHORIZATION_URI_PROPERTY, () -> GOOGLE_PROVIDER.baseUrl() + GOOGLE_AUTHORIZATION_PATH);
    registry.add(GOOGLE_TOKEN_URI_PROPERTY, () -> GOOGLE_PROVIDER.baseUrl() + GOOGLE_TOKEN_PATH);
    registry.add(GOOGLE_JWK_SET_URI_PROPERTY, () -> GOOGLE_PROVIDER.baseUrl() + GOOGLE_JWK_SET_PATH);
    registry.add(GOOGLE_USER_INFO_URI_PROPERTY, () -> GOOGLE_PROVIDER.baseUrl() + GOOGLE_USER_INFO_PATH);
    registry.add(GOOGLE_USER_NAME_ATTRIBUTE_PROPERTY, () -> GOOGLE_USER_NAME_ATTRIBUTE);
    registry.add(GOOGLE_ISSUER_URI_PROPERTY, () -> GOOGLE_ISSUER_URI);
  }

  @Test
  void shouldCompleteGoogleCallbackAndPersistNormalizedAvatar() throws IOException {
    var response = completeGoogleLogin(MOCK_AUTHORIZATION_CODE);

    assertThat(response.statusCode()).isEqualTo(HttpStatus.FOUND.value());
    assertThat(response.getHeader("Location"))
      .isEqualTo("http://localhost:4200/oauth/callback");
    assertThat(response.getCookie(REFRESH_COOKIE)).isNotBlank();

    var user = userRepository.findByEmail(GOOGLE_EMAIL).orElseThrow();
    assertThat(user.getPasswordHash()).isNull();
    assertThat(user.isEmailVerified()).isTrue();
    assertThat(oauthIdentityRepository
      .findByProviderAndProviderSubject(OAuthProvider.GOOGLE, GOOGLE_SUBJECT))
      .hasValueSatisfying(identity -> assertThat(identity.getUser().getId()).isEqualTo(user.getId()));

    var avatar = avatarRepository.findByUserId(user.getId()).orElseThrow();
    assertThat(ImageIO.read(new java.io.ByteArrayInputStream(avatar.getContent())))
      .extracting(BufferedImage::getWidth, BufferedImage::getHeight)
      .containsExactly(256, 256);
    verify(googleAvatarImageClient).download(GOOGLE_PICTURE_URL);
  }

  @Test
  void shouldNotReplaceExistingAvatarOnSubsequentGoogleLogin() {
    completeGoogleLogin(MOCK_AUTHORIZATION_CODE);
    var user = userRepository.findByEmail(GOOGLE_EMAIL).orElseThrow();
    var originalAvatar = avatarRepository.findByUserId(user.getId()).orElseThrow().getContent();
    clearInvocations(googleAvatarImageClient);

    var response = completeGoogleLogin(MOCK_AUTHORIZATION_CODE);

    assertThat(response.statusCode()).isEqualTo(HttpStatus.FOUND.value());
    assertThat(avatarRepository.findByUserId(user.getId()).orElseThrow().getContent())
      .isEqualTo(originalAvatar);
    verify(googleAvatarImageClient, never()).download(anyString());
  }

  @Test
  void shouldCompleteGoogleLoginWhenPictureIsMissing() {
    var response = completeGoogleLogin(MOCK_NO_PICTURE_AUTHORIZATION_CODE);

    assertThat(response.statusCode()).isEqualTo(HttpStatus.FOUND.value());
    var user = userRepository.findByEmail(GOOGLE_NO_PICTURE_EMAIL).orElseThrow();
    assertThat(avatarRepository.findByUserId(user.getId())).isEmpty();
    verify(googleAvatarImageClient, never()).download(anyString());
  }

  private Response completeGoogleLogin(String authorizationCode) {
    var authorizationResponse = startAuthorization();
    var authorizationUri = URI.create(authorizationResponse.getHeader("Location"));
    var cookieValue = cookieValue(authorizationResponse);
    var state = queryParameter(authorizationUri, "state");

    assertThat(authorizationUri.getHost()).isEqualTo("localhost");
    assertThat(authorizationUri.getPort()).isEqualTo(GOOGLE_PROVIDER.port());
    assertThat(queryParameter(authorizationUri, "nonce")).isEqualTo(GOOGLE_NONCE);

    return given()
      .port(port)
      .cookie(AUTHORIZATION_COOKIE, cookieValue)
      .queryParam("code", authorizationCode)
      .queryParam("state", state)
      .redirects()
      .follow(false)
      .when()
      .get(CALLBACK_ENDPOINT)
      .then()
      .extract()
      .response();
  }

  private Response startAuthorization() {
    return given()
      .port(port)
      .redirects()
      .follow(false)
      .when()
      .get(AUTHORIZATION_ENDPOINT)
      .then()
      .statusCode(HttpStatus.FOUND.value())
      .extract()
      .response();
  }

  private String cookieValue(Response response) {
    var header = response.getHeader("Set-Cookie");
    var prefix = AUTHORIZATION_COOKIE + "=";
    var start = header.indexOf(prefix) + prefix.length();
    var end = header.indexOf(';', start);
    return header.substring(start, end);
  }

  private static String queryParameter(URI uri, String name) {
    return Arrays.stream(uri.getRawQuery().split("&"))
      .map(parameter -> parameter.split("=", 2))
      .filter(parameter -> parameter.length == 2 && parameter[0].equals(name))
      .map(parameter -> URLDecoder.decode(parameter[1], StandardCharsets.UTF_8))
      .findFirst()
      .orElseThrow();
  }

  private static WireMockServer startGoogleProvider() {
    var server = new WireMockServer(
      wireMockConfig()
        .dynamicPort()
        .usingFilesUnderClasspath("wiremock/google"));
    server.start();
    return server;
  }

}
