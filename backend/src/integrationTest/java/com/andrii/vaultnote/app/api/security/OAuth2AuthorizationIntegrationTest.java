package com.andrii.vaultnote.app.api.security;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import com.andrii.vaultnote.support.AbstractBaseIntegrationTest;
import io.restassured.response.Response;
import java.net.URI;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class OAuth2AuthorizationIntegrationTest extends AbstractBaseIntegrationTest {

  private static final String AUTHORIZATION_ENDPOINT = "/oauth2/authorization/google";
  private static final String CALLBACK_ENDPOINT = "/login/oauth2/code/google";
  private static final String COOKIE_NAME = "vaultnote_oauth2_authorization_request";

  @Test
  void shouldRedirectToGoogleWithStateAndPkce() {
    var response = startAuthorization();
    var location = response.getHeader("Location");
    var authorizationUri = URI.create(location);
    var query = authorizationUri.getRawQuery();

    assertThat(response.statusCode()).isEqualTo(HttpStatus.FOUND.value());
    assertThat(authorizationUri.getScheme()).isEqualTo("https");
    assertThat(authorizationUri.getHost()).isEqualTo("accounts.google.com");
    assertThat(query)
      .contains("client_id=integration-google-client-id")
      .contains("response_type=code")
      .contains("scope=openid%20profile%20email")
      .contains("state=")
      .contains("code_challenge=")
      .contains("code_challenge_method=S256")
      .contains("redirect_uri=http://localhost:" + port
        + "/login/oauth2/code/google");
    assertThat(response.getHeader("Set-Cookie"))
      .contains(COOKIE_NAME + "=")
      .contains("Max-Age=300")
      .contains("Path=/login/oauth2/code")
      .contains("HttpOnly")
      .contains("SameSite=Lax");
    assertThat(response.getHeaders().getValues("Set-Cookie"))
      .noneMatch(header -> header.startsWith("JSESSIONID="));
  }

  @Test
  void shouldRejectCallbackWithWrongState() {
    var authorizationResponse = startAuthorization();
    var cookieValue = cookieValue(authorizationResponse);

    var response = given()
      .port(port)
      .cookie(COOKIE_NAME, cookieValue)
      .queryParam("code", "unused-code")
      .queryParam("state", "wrong-state")
      .redirects()
      .follow(false)
      .when()
      .get(CALLBACK_ENDPOINT)
      .then()
      .extract()
      .response();

    assertThat(response.statusCode()).isEqualTo(HttpStatus.FOUND.value());
    assertThat(response.getHeader("Location")).contains("/login?error");
    assertThat(response.getHeaders().getValues("Set-Cookie"))
      .noneMatch(header -> header.startsWith("JSESSIONID="))
      .anyMatch(header -> header.contains(COOKIE_NAME + "=") && header.contains("Max-Age=0"));
  }

  private Response startAuthorization() {
    return given()
      .port(port)
      .redirects()
      .follow(false)
      .when()
      .get(AUTHORIZATION_ENDPOINT)
      .then()
      .extract()
      .response();
  }

  private String cookieValue(Response response) {
    var header = response.getHeader("Set-Cookie");
    var prefix = COOKIE_NAME + "=";
    var start = header.indexOf(prefix) + prefix.length();
    var end = header.indexOf(';', start);
    return header.substring(start, end);
  }
}
