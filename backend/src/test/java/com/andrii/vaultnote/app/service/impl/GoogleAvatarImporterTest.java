package com.andrii.vaultnote.app.service.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.andrii.vaultnote.app.security.oauth2.GoogleAvatarImageClient;
import com.andrii.vaultnote.users.infrastructure.persistence.entity.UserAvatarEntity;
import com.andrii.vaultnote.users.infrastructure.persistence.repository.UserAvatarJpaRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GoogleAvatarImporterTest {

  private static final Long USER_ID = 10L;
  private static final String PICTURE_URL = "https://lh3.googleusercontent.com/avatar";
  private static final byte[] DOWNLOADED_CONTENT = {1, 2, 3};
  private static final byte[] NORMALIZED_CONTENT = {4, 5, 6};

  @Mock
  GoogleAvatarImageClient imageClient;
  @Mock
  AvatarImageNormalizer imageNormalizer;
  @Mock
  UserAvatarJpaRepository avatarRepository;

  @InjectMocks
  GoogleAvatarImporter importer;

  @Test
  void shouldDownloadNormalizeAndPersistAvatar() {
    var normalized = new AvatarImageNormalizer.NormalizedAvatar(NORMALIZED_CONTENT);

    when(avatarRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
    when(imageClient.download(PICTURE_URL)).thenReturn(DOWNLOADED_CONTENT);
    when(imageNormalizer.normalize(DOWNLOADED_CONTENT)).thenReturn(normalized);

    importer.importIfAvailable(USER_ID, PICTURE_URL);

    var avatarCaptor = ArgumentCaptor.forClass(UserAvatarEntity.class);
    verify(avatarRepository).saveAndFlush(avatarCaptor.capture());
    var savedAvatar = avatarCaptor.getValue();
    org.assertj.core.api.Assertions.assertThat(savedAvatar.getUserId()).isEqualTo(USER_ID);
    org.assertj.core.api.Assertions.assertThat(savedAvatar.getContent())
      .isEqualTo(NORMALIZED_CONTENT);
    org.assertj.core.api.Assertions.assertThat(savedAvatar.getByteSize())
      .isEqualTo(NORMALIZED_CONTENT.length);
  }

  @Test
  void shouldSkipMissingPicture() {
    importer.importIfAvailable(USER_ID, null);

    verifyNoInteractions(imageClient, imageNormalizer, avatarRepository);
  }

  @Test
  void shouldNotReplaceExistingAvatar() {
    when(avatarRepository.findByUserId(USER_ID))
      .thenReturn(Optional.of(UserAvatarEntity.builder().userId(USER_ID).build()));

    importer.importIfAvailable(USER_ID, PICTURE_URL);

    verifyNoInteractions(imageClient, imageNormalizer);
    verify(avatarRepository, never()).saveAndFlush(any(UserAvatarEntity.class));
  }

  @Test
  void shouldIgnoreDownloadOrValidationFailure() {
    when(avatarRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
    when(imageClient.download(PICTURE_URL)).thenThrow(new IllegalStateException("unavailable"));

    importer.importIfAvailable(USER_ID, PICTURE_URL);

    verify(avatarRepository, never()).saveAndFlush(any(UserAvatarEntity.class));
  }
}
