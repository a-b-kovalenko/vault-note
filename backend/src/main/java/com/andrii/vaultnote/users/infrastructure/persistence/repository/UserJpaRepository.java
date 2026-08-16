package com.andrii.vaultnote.users.infrastructure.persistence.repository;

import com.andrii.vaultnote.users.infrastructure.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserJpaRepository extends JpaRepository<UserEntity, Long> {

  @EntityGraph(attributePaths = "roles")
  Optional<UserEntity> findByEmail(String email);

  @EntityGraph(attributePaths = "roles")
  Optional<UserEntity> findByEmailIgnoreCase(String email);

  @EntityGraph(attributePaths = "roles")
  Optional<UserEntity> findUserWithRolesById(Long id);

  boolean existsByEmail(String email);
}
