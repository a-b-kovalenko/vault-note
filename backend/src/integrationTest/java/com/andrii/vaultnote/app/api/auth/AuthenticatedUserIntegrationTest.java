package com.andrii.vaultnote.app.api.auth;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import com.andrii.vaultnote.app.api.auth.dto.CurrentUserResponse;
import com.andrii.vaultnote.app.security.AccessTokenGenerator;
import com.andrii.vaultnote.support.AbstractBaseIntegrationTest;
import com.andrii.vaultnote.users.domain.UserRole;
import com.andrii.vaultnote.users.infrastructure.persistence.entity.UserEntity;
import com.andrii.vaultnote.users.infrastructure.persistence.repository.UserJpaRepository;
import com.github.database.rider.core.api.dataset.DataSet;
import java.util.EnumSet;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

@DataSet(value = "auth-baseline.yml", skipCleaningFor = {"databasechangelog", "databasechangeloglock"})
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
class AuthenticatedUserIntegrationTest extends AbstractBaseIntegrationTest {

  private static final String CURRENT_USER_ENDPOINT = "/api/v1/auth/me";

  UserJpaRepository userRepository;
  AccessTokenGenerator accessTokenGenerator;

  @Autowired
  AuthenticatedUserIntegrationTest(
    UserJpaRepository userRepository,
    AccessTokenGenerator accessTokenGenerator) {
    this.userRepository = userRepository;
    this.accessTokenGenerator = accessTokenGenerator;
  }

  /**
   * Returns the current user when a valid access token is supplied.
   *
   * <p>
   * The endpoint must validate the JWT signature and claims before returning the
   * subject and roles from the token.
   */
  @Test
  void shouldReturnAuthenticatedUserFromValidAccessToken() {
    var user = userRepository.saveAndFlush(UserEntity.builder()
      .email("authenticated@example.com")
      .displayName("Authenticated User")
      .passwordHash("password-hash")
      .emailVerified(true)
      .roles(EnumSet.of(UserRole.USER))
      .build());
    var accessToken = accessTokenGenerator.generate(user).rawValue();

    var response = given()
      .port(port)
      .auth()
      .oauth2(accessToken)
      .when()
      .get(CURRENT_USER_ENDPOINT)
      .then()
      .statusCode(HttpStatus.OK.value())
      .extract()
      .as(CurrentUserResponse.class);

    assertThat(response.userId()).isEqualTo(user.getId());
    assertThat(response.roles()).containsExactly(UserRole.USER.name());
  }

  /**
   * Rejects a request without a bearer access token.
   *
   * <p>
   * The endpoint must return {@code 401 Unauthorized}.
   */
  @Test
  void shouldRejectRequestWithoutAccessToken() {
    given()
      .port(port)
      .when()
      .get(CURRENT_USER_ENDPOINT)
      .then()
      .statusCode(HttpStatus.UNAUTHORIZED.value());
  }
}
