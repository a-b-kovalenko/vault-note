package com.andrii.vaultnote.users.infrastructure.persistence.entity;

import com.andrii.vaultnote.users.domain.UserRole;
import com.andrii.vaultnote.users.infrastructure.persistence.converter.UserRoleConverter;
import jakarta.persistence.Column;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Convert;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "users")
@Getter
@Builder(toBuilder = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class UserEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  Long id;

  @Column(nullable = false, length = 320, unique = true)
  String email;

  @Column(name = "display_name", nullable = false, length = 100)
  String displayName;

  @Column(name = "password_hash", nullable = false)
  String passwordHash;

  @Column(name = "email_verified", nullable = false)
  boolean emailVerified;

  @ElementCollection(fetch = FetchType.LAZY)
  @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
  @Column(name = "role_code", nullable = false)
  @Convert(converter = UserRoleConverter.class)
  @Builder.Default
  Set<UserRole> roles = new HashSet<>();

  @Column(name = "created_at", nullable = false, updatable = false)
  @CreationTimestamp
  Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  @UpdateTimestamp
  Instant updatedAt;
}
