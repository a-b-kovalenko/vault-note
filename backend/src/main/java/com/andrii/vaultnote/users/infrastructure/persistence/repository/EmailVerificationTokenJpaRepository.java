package com.andrii.vaultnote.users.infrastructure.persistence.repository;

import com.andrii.vaultnote.users.infrastructure.persistence.entity.EmailVerificationTokenEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmailVerificationTokenJpaRepository
    extends
      JpaRepository<EmailVerificationTokenEntity, Long> {

  Optional<EmailVerificationTokenEntity> findByTokenHash(String tokenHash);
}
