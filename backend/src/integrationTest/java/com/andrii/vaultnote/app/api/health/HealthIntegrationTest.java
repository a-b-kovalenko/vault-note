package com.andrii.vaultnote.app.api.health;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import com.andrii.vaultnote.support.AbstractBaseIntegrationTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class HealthIntegrationTest extends AbstractBaseIntegrationTest {

  @Test
  void shouldReportApplicationAsHealthy() {
    var status = given()
      .port(port)
      .accept(ContentType.JSON)
      .when()
      .get("/actuator/health")
      .then()
      .statusCode(HttpStatus.OK.value())
      .extract()
      .jsonPath()
      .getString("status");

    assertThat(status).isEqualTo("UP");
  }
}
