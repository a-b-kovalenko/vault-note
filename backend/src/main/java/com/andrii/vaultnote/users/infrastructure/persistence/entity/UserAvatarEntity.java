package com.andrii.vaultnote.users.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "user_avatars")
@Getter
@Builder(toBuilder = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class UserAvatarEntity {

  @Id
  @Column(name = "user_id", nullable = false)
  Long userId;

  @JdbcTypeCode(SqlTypes.VARBINARY)
  @Column(nullable = false)
  byte[] content;

  @Column(name = "byte_size", nullable = false)
  int byteSize;

  @Column(name = "created_at", nullable = false, updatable = false)
  @CreationTimestamp
  Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  @UpdateTimestamp
  Instant updatedAt;

  public void replace(byte[] content, int byteSize) {
    this.content = content;
    this.byteSize = byteSize;
  }
}
