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
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
    registry.add("app.jwt.secret", () -> TEST_JWT_SECRET);
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
