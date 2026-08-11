package com.andrii.vaultnote.users.infrastructure.persistence.repository;

import com.andrii.vaultnote.users.infrastructure.persistence.entity.RefreshTokenEntity;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenEntity, Long> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);

  @Modifying
  @Query("""
      update RefreshTokenEntity token
         set token.revokedAt = :revokedAt
       where token.tokenFamilyId = :tokenFamilyId
         and token.revokedAt is null
      """)
  int revokeActiveByTokenFamilyId(
      @Param("tokenFamilyId") UUID tokenFamilyId,
      @Param("revokedAt") Instant revokedAt);

  @Modifying
  @Query("""
      update RefreshTokenEntity token
         set token.revokedAt = :revokedAt
       where token.id = :tokenId
         and token.revokedAt is null
      """)
  int revokeActiveById(
      @Param("tokenId") Long tokenId,
      @Param("revokedAt") Instant revokedAt);

}
