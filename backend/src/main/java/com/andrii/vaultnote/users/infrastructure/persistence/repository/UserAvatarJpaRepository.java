package com.andrii.vaultnote.users.infrastructure.persistence.repository;

import com.andrii.vaultnote.users.infrastructure.persistence.entity.UserAvatarEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserAvatarJpaRepository extends JpaRepository<UserAvatarEntity, Long> {

  Optional<UserAvatarEntity> findByUserId(Long userId);
}
