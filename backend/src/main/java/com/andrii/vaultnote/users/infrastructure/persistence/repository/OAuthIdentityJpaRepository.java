package com.andrii.vaultnote.users.infrastructure.persistence.repository;

import com.andrii.vaultnote.users.domain.OAuthProvider;
import com.andrii.vaultnote.users.infrastructure.persistence.entity.OAuthIdentityEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OAuthIdentityJpaRepository extends JpaRepository<OAuthIdentityEntity, Long> {

  Optional<OAuthIdentityEntity> findByProviderAndProviderSubject(
    OAuthProvider provider,
    String providerSubject);

  boolean existsByUser_IdAndProvider(Long userId, OAuthProvider provider);

  long countByUser_Id(Long userId);

  long deleteByUser_IdAndProvider(Long userId, OAuthProvider provider);
}
