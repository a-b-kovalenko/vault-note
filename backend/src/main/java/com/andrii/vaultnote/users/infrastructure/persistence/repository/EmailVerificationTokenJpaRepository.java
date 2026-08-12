package com.andrii.vaultnote.users.infrastructure.persistence.repository;

import com.andrii.vaultnote.users.infrastructure.persistence.entity.EmailVerificationTokenEntity;
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
public interface EmailVerificationTokenJpaRepository
  extends
    JpaRepository<EmailVerificationTokenEntity, Long> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<EmailVerificationTokenEntity> findByTokenHash(String tokenHash);

  @Modifying
  @Query("""
    update EmailVerificationTokenEntity token
       set token.usedAt = :usedAt
     where token.user.id = :userId
       and token.usedAt is null
    """)
  int invalidateActiveByUserId(
    @Param("userId") Long userId,
    @Param("usedAt") Instant usedAt);
}
