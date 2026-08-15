package com.andrii.vaultnote.app.api.users;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import com.andrii.vaultnote.app.api.error.ApiErrorResponse;
import com.andrii.vaultnote.app.api.users.dto.UpdateUserProfileRequest;
import com.andrii.vaultnote.app.security.AccessTokenGenerator;
import com.andrii.vaultnote.support.AbstractBaseIntegrationTest;
import com.andrii.vaultnote.users.domain.UserRole;
import com.andrii.vaultnote.users.infrastructure.persistence.entity.UserEntity;
import com.andrii.vaultnote.users.infrastructure.persistence.repository.UserJpaRepository;
import com.github.database.rider.core.api.dataset.DataSet;
import io.restassured.http.ContentType;
import java.util.EnumSet;
import java.util.UUID;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

@DataSet(value = "auth-baseline.yml", skipCleaningFor = {"databasechangelog", "databasechangeloglock"})
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
class UserIntegrationTest extends AbstractBaseIntegrationTest {

  private static final String USERS_ENDPOINT = "/api/v1/users";
  private static final String CURRENT_USER_ENDPOINT = USERS_ENDPOINT + "/me";

  UserJpaRepository userRepository;
  AccessTokenGenerator accessTokenGenerator;

  @Autowired
  UserIntegrationTest(
    UserJpaRepository userRepository,
    AccessTokenGenerator accessTokenGenerator) {
    this.userRepository = userRepository;
    this.accessTokenGenerator = accessTokenGenerator;
  }

  @Test
  void shouldReturnCurrentUserProfile() {
    var user = saveUser("profile-users@example.com", "Profile User", UserRole.USER);
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
      .response();

    assertThat(response.jsonPath().getLong("id")).isEqualTo(user.getId());
    assertThat(response.jsonPath().getString("email")).isEqualTo(user.getEmail());
    assertThat(response.jsonPath().getString("display_name"))
      .isEqualTo(user.getDisplayName());
    assertThat(response.jsonPath().getBoolean("email_verified")).isTrue();
    assertThat(response.jsonPath().getList("roles")).containsExactly("USER");
    assertThat(response.getBody().asString())
      .doesNotContain("passwordHash")
      .doesNotContain("password_hash");
  }

  @Test
  void shouldUpdateCurrentUserDisplayName() {
    var user = saveUser("profile-update@example.com", "Profile User", UserRole.USER);
    var accessToken = accessTokenGenerator.generate(user).rawValue();

    var response = givenWithCsrf()
      .auth()
      .oauth2(accessToken)
      .contentType(ContentType.JSON)
      .body(UpdateUserProfileRequest.builder().displayName("Updated Profile").build())
      .when()
      .patch(CURRENT_USER_ENDPOINT)
      .then()
      .statusCode(HttpStatus.OK.value())
      .extract()
      .response();

    assertThat(response.jsonPath().getString("display_name")).isEqualTo("Updated Profile");
    assertThat(response.jsonPath().getString("email")).isEqualTo(user.getEmail());
    assertThat(response.jsonPath().getBoolean("email_verified")).isTrue();

    var savedUser = userRepository.findById(user.getId()).orElseThrow();
    assertThat(savedUser.getDisplayName()).isEqualTo("Updated Profile");
    assertThat(savedUser.getEmail()).isEqualTo(user.getEmail());
    assertThat(savedUser.isEmailVerified()).isTrue();
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("invalidProfileUpdates")
  void shouldRejectInvalidProfileUpdate(
    String scenario,
    UpdateUserProfileRequest request,
    String expectedCode) {
    var user = saveUser("profile-validation-" + UUID.randomUUID() + "@example.com",
      "Profile User", UserRole.USER);
    var accessToken = accessTokenGenerator.generate(user).rawValue();

    var response = givenWithCsrf()
      .auth()
      .oauth2(accessToken)
      .contentType(ContentType.JSON)
      .body(request)
      .when()
      .patch(CURRENT_USER_ENDPOINT)
      .then()
      .statusCode(HttpStatus.BAD_REQUEST.value())
      .extract()
      .as(ApiErrorResponse.class);

    assertThat(response.code()).isEqualTo("VALIDATION_FAILED");
    assertThat(response.violations())
      .singleElement()
      .satisfies(violation -> {
        assertThat(violation.field()).isEqualTo("display_name");
        assertThat(violation.code()).isEqualTo(expectedCode);
      });
  }

  private static Stream<Arguments> invalidProfileUpdates() {
    return Stream.of(
      Arguments.of(
        "blank display name",
        UpdateUserProfileRequest.builder().displayName(" ").build(),
        "REQUIRED"),
      Arguments.of(
        "display name too long",
        UpdateUserProfileRequest.builder().displayName("a".repeat(101)).build(),
        "INVALID_LENGTH"));
  }

  @Test
  void shouldRejectCurrentUserProfileWithoutAccessToken() {
    given()
      .port(port)
      .when()
      .get(CURRENT_USER_ENDPOINT)
      .then()
      .statusCode(HttpStatus.UNAUTHORIZED.value());
  }

  /**
   * Returns a paginated user list to an administrator without exposing password
   * data.
   *
   * <p>
   * The endpoint must authorize the request through the service method and
   * preserve the pagination contract.
   */
  @Test
  void shouldReturnPaginatedUsersForAdmin() {
    var admin = saveUser("admin-users@example.com", "Admin User", UserRole.ADMIN);
    var accessToken = accessTokenGenerator.generate(admin).rawValue();

    var response = given()
      .port(port)
      .auth()
      .oauth2(accessToken)
      .queryParam("page", 0)
      .queryParam("size", 1)
      .queryParam("sort", "displayName,asc")
      .when()
      .get(USERS_ENDPOINT)
      .then()
      .statusCode(HttpStatus.OK.value())
      .extract()
      .response();

    assertThat(response.jsonPath().getInt("number")).isZero();
    assertThat(response.jsonPath().getInt("size")).isEqualTo(1);
    assertThat(response.jsonPath().getInt("numberOfElements")).isEqualTo(1);
    assertThat(response.jsonPath().getInt("totalElements")).isEqualTo(3);
    assertThat(response.jsonPath().getString("content[0].email")).isEqualTo(admin.getEmail());
    assertThat(response.getBody().asString())
      .doesNotContain("passwordHash")
      .doesNotContain("password_hash");
  }

  /**
   * Rejects a regular user when requesting the administrator user list.
   *
   * <p>
   * The service-level authorization rule must return {@code 403 Forbidden} after
   * the JWT has been authenticated successfully.
   */
  @Test
  void shouldRejectRegularUser() {
    var user = saveUser("regular-users@example.com", "Regular User", UserRole.USER);
    var accessToken = accessTokenGenerator.generate(user).rawValue();

    given()
      .port(port)
      .auth()
      .oauth2(accessToken)
      .when()
      .get(USERS_ENDPOINT)
      .then()
      .statusCode(HttpStatus.FORBIDDEN.value());
  }

  /**
   * Rejects an unauthenticated request before method-level authorization is
   * evaluated.
   *
   * <p>
   * The endpoint must return {@code 401 Unauthorized} when no bearer token is
   * supplied.
   */
  @Test
  void shouldRejectRequestWithoutAccessToken() {
    given()
      .port(port)
      .when()
      .get(USERS_ENDPOINT)
      .then()
      .statusCode(HttpStatus.UNAUTHORIZED.value());
  }

  private UserEntity saveUser(String email, String displayName, UserRole role) {
    return userRepository.saveAndFlush(UserEntity.builder()
      .email(email)
      .displayName(displayName)
      .passwordHash("password-hash")
      .emailVerified(true)
      .roles(EnumSet.of(role))
      .build());
  }
}
