package com.andrii.vaultnote.app.service.impl;

import com.andrii.vaultnote.app.security.oauth2.GoogleAvatarImageClient;
import com.andrii.vaultnote.users.infrastructure.persistence.entity.UserAvatarEntity;
import com.andrii.vaultnote.users.infrastructure.persistence.repository.UserAvatarJpaRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GoogleAvatarImporter {

  GoogleAvatarImageClient imageClient;
  AvatarImageNormalizer imageNormalizer;
  UserAvatarJpaRepository avatarRepository;

  public void importIfAvailable(Long userId, String pictureUrl) {
    if (!StringUtils.hasText(pictureUrl)
      || avatarRepository.findByUserId(userId).isPresent()) {
      return;
    }

    try {
      var normalized = imageNormalizer.normalize(imageClient.download(pictureUrl));
      avatarRepository.saveAndFlush(UserAvatarEntity.builder()
        .userId(userId)
        .content(normalized.content())
        .byteSize(normalized.byteSize())
        .build());
    } catch (RuntimeException exception) {
      log.warn(
        "Google avatar import skipped: userId={}, reason={}",
        userId,
        exception.getClass().getSimpleName());
    }
  }
}
