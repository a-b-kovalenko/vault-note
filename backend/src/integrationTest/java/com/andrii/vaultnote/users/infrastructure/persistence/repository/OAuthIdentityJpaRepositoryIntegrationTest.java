package com.andrii.vaultnote.users.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.andrii.vaultnote.support.AbstractBaseIntegrationTest;
import com.andrii.vaultnote.users.domain.OAuthProvider;
import com.andrii.vaultnote.users.domain.UserRole;
import com.andrii.vaultnote.users.infrastructure.persistence.entity.OAuthIdentityEntity;
import com.andrii.vaultnote.users.infrastructure.persistence.entity.UserEntity;
import com.github.database.rider.core.api.dataset.DataSet;
import java.util.EnumSet;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

@DataSet(value = "auth-baseline.yml", skipCleaningFor = {"databasechangelog", "databasechangeloglock"})
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
class OAuthIdentityJpaRepositoryIntegrationTest extends AbstractBaseIntegrationTest {

  UserJpaRepository userRepository;
  OAuthIdentityJpaRepository oauthIdentityRepository;

  @Autowired
  OAuthIdentityJpaRepositoryIntegrationTest(
    UserJpaRepository userRepository,
    OAuthIdentityJpaRepository oauthIdentityRepository) {
    this.userRepository = userRepository;
    this.oauthIdentityRepository = oauthIdentityRepository;
  }

  @Test
  @Transactional
  void shouldPersistProviderOnlyUserAndFindItsIdentity() {
    var user = userRepository.saveAndFlush(UserEntity.builder()
      .email("oauth-only@example.com")
      .displayName("OAuth Only")
      .emailVerified(true)
      .roles(EnumSet.of(UserRole.USER))
      .build());
    var identity = oauthIdentityRepository.saveAndFlush(OAuthIdentityEntity.builder()
      .user(user)
      .provider(OAuthProvider.GOOGLE)
      .providerSubject("google-subject-1")
      .build());

    var found = oauthIdentityRepository
      .findByProviderAndProviderSubject(OAuthProvider.GOOGLE, "google-subject-1");

    assertThat(user.getPasswordHash()).isNull();
    assertThat(found)
      .hasValueSatisfying(foundIdentity -> assertThat(foundIdentity.getId()).isEqualTo(identity.getId()));
    assertThat(oauthIdentityRepository.existsByUser_IdAndProvider(user.getId(), OAuthProvider.GOOGLE))
      .isTrue();
    assertThat(oauthIdentityRepository.countByUser_Id(user.getId())).isOne();
    assertThat(oauthIdentityRepository.deleteByUser_IdAndProvider(user.getId(), OAuthProvider.GOOGLE))
      .isOne();
    assertThat(oauthIdentityRepository.countByUser_Id(user.getId())).isZero();
  }

  @Test
  void shouldRejectDuplicateProviderSubject() {
    var firstUser = findUser("existing@example.com");
    var secondUser = saveUser("oauth-second@example.com");
    saveIdentity(firstUser, "google-subject-duplicate");

    assertThatThrownBy(() -> saveIdentity(secondUser, "google-subject-duplicate"))
      .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void shouldRejectSecondIdentityFromSameProviderForUser() {
    var user = findUser("existing@example.com");
    saveIdentity(user, "google-subject-first");

    assertThatThrownBy(() -> saveIdentity(user, "google-subject-second"))
      .isInstanceOf(DataIntegrityViolationException.class);
  }

  private UserEntity findUser(String email) {
    return userRepository.findByEmail(email).orElseThrow();
  }

  private UserEntity saveUser(String email) {
    return userRepository.saveAndFlush(UserEntity.builder()
      .email(email)
      .displayName("OAuth User")
      .passwordHash("password-hash")
      .emailVerified(true)
      .roles(EnumSet.of(UserRole.USER))
      .build());
  }

  private OAuthIdentityEntity saveIdentity(UserEntity user, String providerSubject) {
    return oauthIdentityRepository.saveAndFlush(OAuthIdentityEntity.builder()
      .user(user)
      .provider(OAuthProvider.GOOGLE)
      .providerSubject(providerSubject)
      .build());
  }
}
