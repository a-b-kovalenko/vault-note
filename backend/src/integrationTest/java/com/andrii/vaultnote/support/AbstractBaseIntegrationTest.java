package com.andrii.vaultnote.support;

import static io.restassured.RestAssured.given;

import com.andrii.vaultnote.app.mail.MailSender;
import com.github.database.rider.core.api.configuration.DBUnit;
import com.github.database.rider.core.api.configuration.Orthography;
import com.github.database.rider.spring.api.DBRider;
import io.restassured.specification.RequestSpecification;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

@DBRider
@DBUnit(
  schema = "vaultnote",
  caseInsensitiveStrategy = Orthography.LOWERCASE,
  alwaysCleanBefore = true,
  alwaysCleanAfter = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(AbstractBaseIntegrationTest.TestMailConfiguration.class)
public abstract class AbstractBaseIntegrationTest {

  private static final String CSRF_ENDPOINT = "/csrf";
  private static final String CSRF_COOKIE_NAME = "XSRF-TOKEN";
  private static final String CSRF_HEADER_NAME = "X-XSRF-TOKEN";
  private static final String TEST_JWT_SECRET = "integration-test-jwt-secret-that-is-long-enough";
  private static final String DATASOURCE_URL_PROPERTY = "spring.datasource.url";
  private static final String DATASOURCE_USERNAME_PROPERTY = "spring.datasource.username";
  private static final String DATASOURCE_PASSWORD_PROPERTY = "spring.datasource.password";
  private static final String JWT_SECRET_PROPERTY = "app.jwt.secret";
  private static final String GOOGLE_CLIENT_ID_PROPERTY = "spring.security.oauth2.client.registration.google.client-id";
  private static final String GOOGLE_CLIENT_SECRET_PROPERTY = "spring.security.oauth2.client.registration.google.client-secret";
  private static final String GOOGLE_SCOPE_PROPERTY = "spring.security.oauth2.client.registration.google.scope";
  private static final String GOOGLE_REQUIRE_PROOF_KEY_PROPERTY = "spring.security.oauth2.client.registration.google.client-settings.require-proof-key";
  private static final String TEST_GOOGLE_CLIENT_ID = "integration-google-client-id";
  private static final String TEST_GOOGLE_CLIENT_SECRET = "integration-google-client-secret";
  private static final String TEST_GOOGLE_SCOPE = "openid,profile,email";
  private static final String TEST_GOOGLE_REQUIRE_PROOF_KEY = "true";

  protected static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine")
    .withDatabaseName("vault_note")
    .withUsername("user")
    .withPassword("password")
    .withInitScript("db/init.sql");

  static {
    POSTGRES.start();
  }

  @LocalServerPort
  protected int port;

  @DynamicPropertySource
  static void registerPostgresProperties(DynamicPropertyRegistry registry) {
    registry.add(DATASOURCE_URL_PROPERTY, POSTGRES::getJdbcUrl);
    registry.add(DATASOURCE_USERNAME_PROPERTY, POSTGRES::getUsername);
    registry.add(DATASOURCE_PASSWORD_PROPERTY, POSTGRES::getPassword);
    registry.add(JWT_SECRET_PROPERTY, () -> TEST_JWT_SECRET);
    registry.add(GOOGLE_CLIENT_ID_PROPERTY, () -> TEST_GOOGLE_CLIENT_ID);
    registry.add(GOOGLE_CLIENT_SECRET_PROPERTY, () -> TEST_GOOGLE_CLIENT_SECRET);
    registry.add(GOOGLE_SCOPE_PROPERTY, () -> TEST_GOOGLE_SCOPE);
    registry.add(GOOGLE_REQUIRE_PROOF_KEY_PROPERTY, () -> TEST_GOOGLE_REQUIRE_PROOF_KEY);
  }

  protected RequestSpecification givenWithCsrf() {
    var csrfResponse = given()
      .port(port)
      .when()
      .get(CSRF_ENDPOINT)
      .then()
      .statusCode(HttpStatus.OK.value())
      .extract()
      .response();

    return given()
      .port(port)
      .cookie(CSRF_COOKIE_NAME, csrfResponse.getCookie(CSRF_COOKIE_NAME))
      .header(CSRF_HEADER_NAME, csrfResponse.jsonPath().getString("token"));
  }

  @TestConfiguration(proxyBeanMethods = false)
  static class TestMailConfiguration {

    @Bean
    @Primary
    MailSender testMailSender() {
      return Mockito.mock(MailSender.class);
    }
  }
}
