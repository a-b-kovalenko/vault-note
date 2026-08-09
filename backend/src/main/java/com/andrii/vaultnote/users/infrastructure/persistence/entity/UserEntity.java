package com.andrii.vaultnote.users.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "users", schema = "vaultnote")
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

  @Column(name = "created_at", nullable = false, updatable = false)
  @CreationTimestamp
  Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  @UpdateTimestamp
  Instant updatedAt;
}
