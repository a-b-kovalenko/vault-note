package com.andrii.vaultnote.users.infrastructure.persistence.entity;

import com.andrii.vaultnote.users.domain.OAuthProvider;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(
  name = "oauth_identities",
  uniqueConstraints = {
    @UniqueConstraint(
      name = "uk_oauth_identities_provider_subject",
      columnNames = {"provider", "provider_subject"}),
    @UniqueConstraint(
      name = "uk_oauth_identities_user_provider",
      columnNames = {"user_id", "provider"})})
@Getter
@Builder(toBuilder = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class OAuthIdentityEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  UserEntity user;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  OAuthProvider provider;

  @Column(name = "provider_subject", nullable = false)
  String providerSubject;

  @Column(name = "created_at", nullable = false, updatable = false)
  @CreationTimestamp
  Instant createdAt;
}
