package com.andrii.vaultnote.users.infrastructure.persistence.repository;

import com.andrii.vaultnote.users.infrastructure.persistence.entity.PasswordResetTokenEntity;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PasswordResetTokenJpaRepository extends JpaRepository<PasswordResetTokenEntity, Long> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<PasswordResetTokenEntity> findByTokenHash(String tokenHash);

  @Modifying
  @Query("""
      update PasswordResetTokenEntity token
         set token.invalidatedAt = :invalidatedAt
       where token.user.id = :userId
         and token.usedAt is null
         and token.invalidatedAt is null
      """)
  int invalidateActiveByUserId(
      @Param("userId") Long userId,
      @Param("invalidatedAt") Instant invalidatedAt);
}
